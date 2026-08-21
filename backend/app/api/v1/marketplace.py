import logging
import uuid

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.core.deps import get_current_user
from app.db.session import get_db
from app.models.marketplace import Entitlement, Listing, Purchase
from app.models.pack import Pack
from app.models.user import User
from app.schemas.marketplace import (
    ListingCreate,
    ListingDetail,
    ListingOut,
    ListingPage,
    ListingPackSummary,
)
from app.services import marketplace_service, storage

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/marketplace", tags=["marketplace"])


def _pack_summary(pack: Pack, count: int) -> ListingPackSummary:
    return ListingPackSummary(
        id=pack.id,
        title=pack.title,
        description=pack.description,
        cover_url=(
            storage.presigned_get(pack.cover_url) if pack.cover_url else None
        ),
        tags=[t.name for t in pack.tags],
        item_count=count,
    )


def _listing_out(listing: Listing, count: int) -> ListingOut:
    return ListingOut(
        id=listing.id,
        pack=_pack_summary(listing.pack, count),
        seller_display_name=listing.seller.display_name,
        price_cents=listing.price_cents,
        currency=listing.currency,
        downloads=listing.downloads,
        rating_avg=listing.rating_avg,
        created_at=listing.created_at,
    )


@router.post("/listings", response_model=ListingOut, status_code=status.HTTP_201_CREATED)
async def create_listing(
    payload: ListingCreate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> ListingOut:
    pack = await db.get(Pack, payload.pack_id)
    if pack is None or pack.owner_id != current_user.id:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Pack not found")

    existing = await db.scalar(
        select(Listing).where(Listing.pack_id == payload.pack_id)
    )
    if existing is not None:
        raise HTTPException(
            status.HTTP_409_CONFLICT, "This pack already has a listing"
        )

    listing = Listing(
        pack_id=pack.id,
        seller_id=current_user.id,
        price_cents=payload.price_cents,
        currency=payload.currency,
        status="pending",
    )
    db.add(listing)
    # Any user can start selling (plan §0); flag is set on first listing.
    current_user.is_creator = True
    await db.commit()
    await db.refresh(listing)

    _enqueue_moderation(listing.id)
    return _listing_out(listing, 0)


def _enqueue_moderation(listing_id: uuid.UUID) -> None:
    try:
        from app.workers.tasks import moderate_listing

        moderate_listing.delay(str(listing_id))
    except Exception:  # noqa: BLE001 - broker down must not fail the request
        logger.exception(
            "Failed to enqueue moderation for listing %s", listing_id
        )


@router.get("/listings", response_model=ListingPage)
async def browse_listings(
    q: str | None = Query(default=None, max_length=100),
    tag: str | None = Query(default=None, max_length=64),
    sort: str = Query(default="newest"),
    page: int = Query(default=1, ge=1),
    page_size: int = Query(
        default=marketplace_service.DEFAULT_PAGE_SIZE,
        ge=1,
        le=marketplace_service.MAX_PAGE_SIZE,
    ),
    db: AsyncSession = Depends(get_db),
) -> ListingPage:
    sort_key = marketplace_service.parse_sort(sort)
    listings, total = await marketplace_service.search_listings(
        db, q=q, tag=tag, sort=sort_key, page=page, page_size=page_size
    )
    counts = await marketplace_service.item_counts(db, [l.pack_id for l in listings])
    return ListingPage(
        items=[_listing_out(l, counts.get(l.pack_id, 0)) for l in listings],
        total=total,
        page=page,
        page_size=page_size,
    )


@router.get("/listings/{listing_id}", response_model=ListingDetail)
async def get_listing(
    listing_id: uuid.UUID,
    db: AsyncSession = Depends(get_db),
) -> ListingDetail:
    listing = await db.scalar(
        select(Listing)
        .options(
            selectinload(Listing.pack).selectinload(Pack.items),
            selectinload(Listing.seller),
        )
        .where(Listing.id == listing_id)
    )
    if listing is None or listing.status != "approved":
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Listing not found")

    base = _listing_out(listing, len(listing.pack.items))
    previews = [
        storage.presigned_get(i.preview_url)
        for i in listing.pack.items
        if i.preview_url
    ]
    return ListingDetail(
        **base.model_dump(),
        preview_urls=[p for p in previews if p],
    )


@router.post("/listings/{listing_id}/purchase")
async def purchase_listing(
    listing_id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> dict:
    from app.services import stripe_service

    listing = await db.get(Listing, listing_id)
    if listing is None or listing.status != "approved":
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Listing not found")
    if listing.seller_id == current_user.id:
        raise HTTPException(status.HTTP_403_FORBIDDEN, "You cannot buy your own pack")

    already = await db.get(Entitlement, (current_user.id, listing.pack_id))
    if already is not None:
        raise HTTPException(status.HTTP_409_CONFLICT, "You already own this pack")

    # Free packs are granted instantly, no Stripe round-trip.
    if listing.price_cents == 0:
        db.add(Entitlement(buyer_id=current_user.id, pack_id=listing.pack_id))
        db.add(
            Purchase(
                buyer_id=current_user.id,
                listing_id=listing.id,
                amount_cents=0,
                fee_cents=0,
                status="completed",
            )
        )
        listing.downloads += 1
        await db.commit()
        return {"free": True, "checkout_url": None}

    session_id, checkout_url = stripe_service.create_checkout_session(
        listing, current_user
    )
    db.add(
        Purchase(
            buyer_id=current_user.id,
            listing_id=listing.id,
            stripe_checkout_id=session_id,
            amount_cents=listing.price_cents,
            fee_cents=stripe_service.compute_fee(listing.price_cents),
            status="pending",
        )
    )
    await db.commit()
    return {"free": False, "checkout_url": checkout_url}


@router.get("/entitlements/me")
async def my_entitlements(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> list[dict]:
    rows = (
        (
            await db.scalars(
                select(Entitlement)
                .options(selectinload(Entitlement.pack).selectinload(Pack.items))
                .where(Entitlement.buyer_id == current_user.id)
                .order_by(Entitlement.created_at.desc())
            )
        )
        .unique()
        .all()
    )
    return [
        {
            "pack_id": e.pack_id,
            "title": e.pack.title,
            "cover_url": (
                storage.presigned_get(e.pack.cover_url) if e.pack.cover_url else None
            ),
            "items": [
                {
                    "id": i.id,
                    "type": i.type,
                    "file_url": storage.presigned_get(i.file_url),
                    "width": i.width,
                    "height": i.height,
                    "duration_ms": i.duration_ms,
                    "position": i.position,
                }
                for i in e.pack.items
            ],
        }
        for e in rows
    ]


@router.get("/packs/{pack_id}/download")
async def download_pack(
    pack_id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> dict:
    """Presigned manifest for an owned (or free) pack — mobile unzips/downloads."""
    pack = await db.scalar(
        select(Pack)
        .options(selectinload(Pack.items))
        .where(Pack.id == pack_id)
    )
    if pack is None or not pack.is_public:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Pack not found")
    if pack.owner_id != current_user.id:
        listing = await db.scalar(select(Listing).where(Listing.pack_id == pack_id))
        free_or_owned = (listing is not None and listing.price_cents == 0) or (
            await db.get(Entitlement, (current_user.id, pack_id)) is not None
        )
        if not free_or_owned:
            raise HTTPException(
                status.HTTP_403_FORBIDDEN, "You do not own this pack"
            )

    return {
        "pack_id": pack.id,
        "title": pack.title,
        "items": [
            {
                "id": i.id,
                "type": i.type,
                "file_url": storage.presigned_get(i.file_url),
                "preview_url": (
                    storage.presigned_get(i.preview_url) if i.preview_url else None
                ),
                "position": i.position,
            }
            for i in pack.items
        ],
    }

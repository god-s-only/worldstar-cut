import uuid

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.core.deps import get_current_user
from app.db.session import get_db
from app.models.marketplace import Listing
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
    return _listing_out(listing, 0)


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

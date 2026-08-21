import uuid

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.core.deps import get_current_admin
from app.db.session import get_db
from app.models.marketplace import Listing
from app.models.moderation import ReviewQueueItem
from app.models.user import User
from app.schemas.admin import RejectPayload, ReviewItemOut

router = APIRouter(prefix="/admin", tags=["admin"])


def _review_out(item: ReviewQueueItem, listing: Listing | None) -> ReviewItemOut:
    return ReviewItemOut(
        id=item.id,
        listing_id=item.listing_id,
        status=item.status,
        vision_score=item.vision_score,
        created_at=item.created_at,
        pack_title=listing.pack.title if listing else None,
        listing_status=listing.status if listing else None,
    )


@router.get("/review-queue", response_model=list[ReviewItemOut])
async def review_queue(
    queue_status: str = Query(default="pending", pattern="^(pending|approved|rejected)$"),
    admin: User = Depends(get_current_admin),
    db: AsyncSession = Depends(get_db),
) -> list[ReviewItemOut]:
    items = (
        (
            await db.scalars(
                select(ReviewQueueItem)
                .options(selectinload(ReviewQueueItem.listing).selectinload(Listing.pack))
                .where(ReviewQueueItem.status == queue_status)
                .order_by(ReviewQueueItem.created_at.asc())
            )
        )
        .unique()
        .all()
    )
    return [_review_out(i, i.listing) for i in items]


@router.post("/review-queue/{item_id}/approve")
async def approve_listing(
    item_id: uuid.UUID,
    admin: User = Depends(get_current_admin),
    db: AsyncSession = Depends(get_db),
) -> dict:
    item = await _get_pending_item(item_id, db)
    listing = await db.get(Listing, item.listing_id)
    if listing is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Listing no longer exists")

    item.status = "approved"
    item.reviewer_id = admin.id
    item.decided_at = func.now()
    listing.status = "approved"
    listing.reject_reason = None
    await db.commit()
    return {"listing_id": str(listing.id), "status": "approved"}


@router.post("/review-queue/{item_id}/reject")
async def reject_listing(
    item_id: uuid.UUID,
    payload: RejectPayload,
    admin: User = Depends(get_current_admin),
    db: AsyncSession = Depends(get_db),
) -> dict:
    item = await _get_pending_item(item_id, db)
    listing = await db.get(Listing, item.listing_id)
    if listing is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Listing no longer exists")

    item.status = "rejected"
    item.reviewer_id = admin.id
    item.decided_at = func.now()
    listing.status = "rejected"
    listing.reject_reason = payload.reason
    await db.commit()
    return {"listing_id": str(listing.id), "status": "rejected"}


async def _get_pending_item(
    item_id: uuid.UUID, db: AsyncSession
) -> ReviewQueueItem:
    item = await db.get(ReviewQueueItem, item_id)
    if item is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Review item not found")
    if item.status != "pending":
        raise HTTPException(
            status.HTTP_409_CONFLICT, f"Already decided ({item.status})"
        )
    return item

"""Background tasks: moderation, previews (plan §6, §7)."""
import asyncio
import logging

from sqlalchemy import select

from app.models.marketplace import Listing
from app.models.moderation import ReviewQueueItem
from app.services import moderation_service, storage
from app.workers import celery

logger = logging.getLogger(__name__)


@celery.task(name="moderation.moderate_listing", bind=True, max_retries=3)
def moderate_listing(self, listing_id: str):
    try:
        return asyncio.run(_moderate(listing_id))
    except Exception as exc:  # noqa: BLE001 - retry transient failures
        logger.exception("moderate_listing failed for %s", listing_id)
        raise self.retry(exc=exc, countdown=10) from exc


async def _moderate(listing_id: str) -> str:
    from app.db.session import AsyncSessionLocal

    async with AsyncSessionLocal() as db:
        listing = await db.scalar(
            select(Listing).where(Listing.id == listing_id)
        )
        if listing is None:
            return "listing-missing"

        score: float | None = None
        labels: list[dict] = []
        provider = "no-previews"

        for item in listing.pack.items:
            if not item.preview_url:
                continue
            data = storage.get_object_bytes(
                item.preview_url, bucket=storage.previews_bucket()
            )
            if data is None:
                continue
            result = moderation_service.check_image(data)
            provider = result["provider"]
            labels.extend(result["labels"])
            if result["score"] is not None:
                score = max(score or 0.0, result["score"])

        action = moderation_service.decide_action(score)

        db.add(
            ReviewQueueItem(
                listing_id=listing.id,
                vision_score={
                    "score": score,
                    "labels": labels[:50],
                    "provider": provider,
                },
                status="pending" if action == "manual" else action,
            )
        )

        if action == "reject":
            listing.status = "rejected"
            listing.reject_reason = "Failed automated content check"
        elif action == "approve":
            listing.status = "approved"

        await db.commit()
        return f"{action}:score={score}"

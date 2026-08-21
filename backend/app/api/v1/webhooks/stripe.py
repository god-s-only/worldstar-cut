"""Stripe webhook receiver (plan §5). Public endpoint — signature-verified."""
import logging

from fastapi import APIRouter, Depends, Header, HTTPException, Request, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.session import get_db
from app.models.marketplace import Entitlement, Listing, Purchase

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/webhooks", tags=["webhooks"])


@router.post("/stripe", status_code=status.HTTP_200_OK)
async def stripe_webhook(
    request: Request,
    stripe_signature: str = Header(default=""),
    db: AsyncSession = Depends(get_db),
) -> dict:
    from app.services import stripe_service

    payload = await request.body()
    try:
        event = stripe_service.construct_webhook_event(payload, stripe_signature)
    except Exception:  # noqa: BLE001 - bad signature/payload
        raise HTTPException(status.HTTP_400_BAD_REQUEST, "Invalid webhook signature")

    if event["type"] == "checkout.session.completed":
        await _handle_checkout_completed(event["data"]["object"], db)
    else:
        logger.info("Ignored Stripe event type=%s", event["type"])
    return {"received": True}


async def _handle_checkout_completed(session: dict, db: AsyncSession) -> None:
    metadata = session.get("metadata") or {}
    listing_id = metadata.get("listing_id")
    buyer_id = metadata.get("buyer_id")
    if not listing_id or not buyer_id:
        logger.error("checkout.session.completed without expected metadata")
        return

    purchase = await db.scalar(
        select(Purchase).where(Purchase.stripe_checkout_id == session["id"])
    )
    if purchase is None:
        purchase = Purchase(
            buyer_id=buyer_id,
            listing_id=listing_id,
            stripe_checkout_id=session["id"],
            amount_cents=session.get("amount_total") or 0,
            fee_cents=0,
        )
        db.add(purchase)
    if purchase.status == "completed":
        return  # idempotent replay

    purchase.status = "completed"

    listing = await db.get(Listing, listing_id)
    if listing is None:
        logger.error("Completed checkout for missing listing %s", listing_id)
        return

    existing = await db.get(Entitlement, (buyer_id, listing.pack_id))
    if existing is None:
        db.add(Entitlement(buyer_id=buyer_id, pack_id=listing.pack_id))
        listing.downloads += 1

    await db.commit()

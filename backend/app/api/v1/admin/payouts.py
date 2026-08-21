import uuid

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.deps import get_current_admin
from app.db.session import get_db
from app.models.marketplace import Payout
from app.models.user import User
from app.schemas.admin import PayoutCreate, PayoutOut

router = APIRouter(prefix="/admin", tags=["admin"])


@router.post("/payouts", response_model=PayoutOut, status_code=status.HTTP_201_CREATED)
async def create_payout(
    payload: PayoutCreate,
    admin: User = Depends(get_current_admin),
    db: AsyncSession = Depends(get_db),
) -> PayoutOut:
    """Manual seller payout via Stripe Connect transfer (plan §6, step 6)."""
    from app.services import stripe_service

    seller = await db.get(User, payload.seller_id)
    if seller is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Seller not found")
    if not seller.stripe_account_id:
        raise HTTPException(
            status.HTTP_409_CONFLICT, "Seller has no Stripe Connect account"
        )

    payout = Payout(
        seller_id=seller.id,
        amount_cents=payload.amount_cents,
        currency=payload.currency,
        status="pending",
    )
    db.add(payout)
    await db.flush()

    try:
        import stripe as stripe_sdk

        stripe_sdk.api_key = stripe_service._api_key()
        transfer = stripe_sdk.Transfer.create(
            amount=payload.amount_cents,
            currency=payload.currency,
            destination=seller.stripe_account_id,
            metadata={"payout_id": str(payout.id)},
        )
    except Exception:  # noqa: BLE001 - record failure, surface 502
        payout.status = "failed"
        await db.commit()
        raise HTTPException(
            status.HTTP_502_BAD_GATEWAY, "Stripe transfer failed"
        )

    payout.stripe_transfer_id = transfer.id
    payout.status = "completed"
    await db.commit()
    await db.refresh(payout)
    return PayoutOut(
        id=payout.id,
        seller_id=payout.seller_id,
        amount_cents=payout.amount_cents,
        currency=payout.currency,
        stripe_transfer_id=payout.stripe_transfer_id,
        status=payout.status,
        created_at=payout.created_at,
    )


@router.get("/payouts", response_model=list[PayoutOut])
async def list_payouts(
    admin: User = Depends(get_current_admin),
    db: AsyncSession = Depends(get_db),
) -> list[PayoutOut]:
    payouts = (
        await db.scalars(select(Payout).order_by(Payout.created_at.desc()))
    ).all()
    return [
        PayoutOut(
            id=p.id,
            seller_id=p.seller_id,
            amount_cents=p.amount_cents,
            currency=p.currency,
            stripe_transfer_id=p.stripe_transfer_id,
            status=p.status,
            created_at=p.created_at,
        )
        for p in payouts
    ]

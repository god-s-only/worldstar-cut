"""Stripe + Stripe Connect Express integration (plan §3, §5)."""
import logging

import stripe
from fastapi import HTTPException

from app.core.config import settings
from app.models.marketplace import Listing
from app.models.user import User

logger = logging.getLogger(__name__)


def _api_key() -> str:
    if not settings.STRIPE_SECRET_KEY:
        raise HTTPException(
            503, "Payments are not configured (STRIPE_SECRET_KEY missing)"
        )
    return settings.STRIPE_SECRET_KEY


def compute_fee(amount_cents: int) -> int:
    """Platform application fee taken from the seller's charge."""
    return round(amount_cents * settings.STRIPE_PLATFORM_FEE_PERCENT / 100)


def create_connect_account(user: User) -> str:
    stripe.api_key = _api_key()
    account = stripe.Account.create(
        type="express",
        email=user.email,
        capabilities={
            "transfers": {"requested": True},
            "card_payments": {"requested": True},
        },
    )
    return account.id


def create_onboarding_link(stripe_account_id: str) -> str:
    stripe.api_key = _api_key()
    base = settings.APP_BASE_URL
    link = stripe.AccountLink.create(
        account=stripe_account_id,
        refresh_url=f"{base}/marketplace/seller/refresh",
        return_url=f"{base}/marketplace/seller/onboarded",
        type="account_onboarding",
    )
    return link.url


def create_checkout_session(listing: Listing, buyer: User) -> str:
    """Destination charge: money goes to the seller's Connect account minus fee."""
    stripe.api_key = _api_key()
    if not listing.seller.stripe_account_id:
        raise HTTPException(409, "Seller has not completed payout onboarding")

    session = stripe.checkout.Session.create(
        mode="payment",
        line_items=[
            {
                "price_data": {
                    "currency": listing.currency,
                    "unit_amount": listing.price_cents,
                    "product_data": {"name": listing.pack.title},
                },
                "quantity": 1,
            }
        ],
        success_url=(
            f"{settings.APP_BASE_URL}/marketplace/purchase/success"
            "?session_id={CHECKOUT_SESSION_ID}"
        ),
        cancel_url=f"{settings.APP_BASE_URL}/marketplace/purchase/cancelled",
        metadata={
            "listing_id": str(listing.id),
            "buyer_id": str(buyer.id),
        },
        payment_intent_data={
            "application_fee_amount": compute_fee(listing.price_cents),
            "transfer_data": {"destination": listing.seller.stripe_account_id},
        },
    )
    return session.id, session.url


def construct_webhook_event(payload: bytes, signature: str):
    if not settings.STRIPE_WEBHOOK_SECRET:
        raise HTTPException(503, "Webhook secret is not configured")
    return stripe.Webhook.construct_event(
        payload, signature, settings.STRIPE_WEBHOOK_SECRET
    )

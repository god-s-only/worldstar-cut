import uuid
from datetime import datetime

from pydantic import BaseModel, Field


class RejectPayload(BaseModel):
    reason: str = Field(min_length=3, max_length=500)


class ReviewItemOut(BaseModel):
    id: uuid.UUID
    listing_id: uuid.UUID
    status: str
    vision_score: dict | None
    created_at: datetime
    pack_title: str | None = None
    listing_status: str | None = None


class PayoutCreate(BaseModel):
    seller_id: uuid.UUID
    amount_cents: int = Field(gt=0, le=100_000_000)
    currency: str = Field(default="usd", pattern=r"^[a-z]{3}$")


class PayoutOut(BaseModel):
    id: uuid.UUID
    seller_id: uuid.UUID
    amount_cents: int
    currency: str
    stripe_transfer_id: str | None
    status: str
    created_at: datetime

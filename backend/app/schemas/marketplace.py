import uuid
from datetime import datetime

from pydantic import BaseModel, Field


class ListingCreate(BaseModel):
    pack_id: uuid.UUID
    price_cents: int = Field(ge=0, le=100_000_000)  # 0 = free
    currency: str = Field(default="usd", pattern=r"^[a-z]{3}$")


class ListingPackSummary(BaseModel):
    id: uuid.UUID
    title: str
    description: str | None
    cover_url: str | None
    tags: list[str]
    item_count: int


class ListingOut(BaseModel):
    id: uuid.UUID
    pack: ListingPackSummary
    seller_display_name: str
    price_cents: int
    currency: str
    downloads: int
    rating_avg: float | None
    created_at: datetime


class ListingDetail(ListingOut):
    preview_urls: list[str] = []


class ListingPage(BaseModel):
    items: list[ListingOut]
    total: int
    page: int
    page_size: int

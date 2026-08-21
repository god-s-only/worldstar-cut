import uuid
from datetime import datetime

from pydantic import BaseModel, Field


class PackCreate(BaseModel):
    title: str = Field(min_length=1, max_length=120)
    description: str | None = Field(default=None, max_length=2000)
    tags: list[str] = Field(default_factory=list)
    is_public: bool = False


class PackUpdate(BaseModel):
    title: str | None = Field(default=None, min_length=1, max_length=120)
    description: str | None = Field(default=None, max_length=2000)
    tags: list[str] | None = None
    is_public: bool | None = None


class PackItemOut(BaseModel):
    id: uuid.UUID
    type: str
    # Presigned URLs filled in at read time; None when storage is unavailable.
    file_url: str | None
    preview_url: str | None
    width: int | None
    height: int | None
    duration_ms: int | None
    position: int


class PackOut(BaseModel):
    id: uuid.UUID
    owner_id: uuid.UUID
    title: str
    description: str | None
    cover_url: str | None
    is_public: bool
    tags: list[str]
    created_at: datetime
    updated_at: datetime
    items: list[PackItemOut] = []


class PackItemCreateResult(BaseModel):
    item: PackItemOut

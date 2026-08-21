import uuid
from datetime import datetime
from typing import Any

from sqlalchemy import (
    Boolean,
    Column,
    Computed,
    DateTime,
    ForeignKey,
    Integer,
    String,
    Table,
    Text,
    func,
)
from sqlalchemy.dialects.postgresql import TSVECTOR, UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.base import Base

pack_tags = Table(
    "pack_tags",
    Base.metadata,
    Column("pack_id", ForeignKey("packs.id", ondelete="CASCADE"), primary_key=True),
    Column("tag_id", ForeignKey("tags.id", ondelete="CASCADE"), primary_key=True),
)


class Tag(Base):
    __tablename__ = "tags"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    name: Mapped[str] = mapped_column(String(64), unique=True, nullable=False)

    def __repr__(self) -> str:
        return f"<Tag {self.name!r}>"


class Pack(Base):
    __tablename__ = "packs"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=uuid.uuid4
    )
    owner_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True
    )
    title: Mapped[str] = mapped_column(String(120), nullable=False)
    description: Mapped[str | None] = mapped_column(Text, nullable=True)
    cover_url: Mapped[str | None] = mapped_column(Text, nullable=True)
    is_public: Mapped[bool] = mapped_column(
        Boolean, nullable=False, default=False, server_default="false"
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        nullable=False,
        server_default=func.now(),
        onupdate=func.now(),
    )
    # Full-text search over title + description (plan §8); tags searched via join.
    search_vector: Mapped[Any] = mapped_column(
        TSVECTOR,
        Computed(
            "to_tsvector('english', coalesce(title, '') || ' ' || coalesce(description, ''))",
            persisted=True,
        ),
    )

    items: Mapped[list["PackItem"]] = relationship(
        back_populates="pack",
        cascade="all, delete-orphan",
        order_by="PackItem.position",
    )
    tags: Mapped[list["Tag"]] = relationship(secondary=pack_tags, lazy="selectin")

    def __repr__(self) -> str:
        return f"<Pack id={self.id} title={self.title!r}>"


class PackItem(Base):
    __tablename__ = "pack_items"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=uuid.uuid4
    )
    pack_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("packs.id", ondelete="CASCADE"), nullable=False, index=True
    )
    # "sticker" (png/webp) or "animation" (lottie json)
    type: Mapped[str] = mapped_column(String(16), nullable=False)
    # Object keys (not signed URLs); presigned at read time.
    file_url: Mapped[str] = mapped_column(Text, nullable=False)
    preview_url: Mapped[str | None] = mapped_column(Text, nullable=True)
    width: Mapped[int | None] = mapped_column(Integer, nullable=True)
    height: Mapped[int | None] = mapped_column(Integer, nullable=True)
    duration_ms: Mapped[int | None] = mapped_column(Integer, nullable=True)
    # Plan calls this "order"; renamed to avoid the SQL reserved word.
    position: Mapped[int] = mapped_column(Integer, nullable=False, default=0)

    pack: Mapped["Pack"] = relationship(back_populates="items")

    def __repr__(self) -> str:
        return f"<PackItem id={self.id} type={self.type!r}>"

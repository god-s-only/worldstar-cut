import uuid
from datetime import datetime

from sqlalchemy import DateTime, ForeignKey, Integer, String, Text, func
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.base import Base


class Listing(Base):
    __tablename__ = "marketplace_listings"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=uuid.uuid4
    )
    pack_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("packs.id", ondelete="CASCADE"), nullable=False, unique=True
    )
    seller_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True
    )
    # 0 = free pack
    price_cents: Mapped[int] = mapped_column(Integer, nullable=False)
    currency: Mapped[str] = mapped_column(
        String(8), nullable=False, default="usd", server_default="usd"
    )
    # pending / approved / rejected (moderation gate, plan §6)
    status: Mapped[str] = mapped_column(
        String(16), nullable=False, default="pending", server_default="pending"
    )
    reject_reason: Mapped[str | None] = mapped_column(Text, nullable=True)
    downloads: Mapped[int] = mapped_column(
        Integer, nullable=False, default=0, server_default="0"
    )
    rating_avg: Mapped[float | None] = mapped_column(nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )

    pack: Mapped["Pack"] = relationship(lazy="selectin")
    seller: Mapped["User"] = relationship(lazy="selectin")

    def __repr__(self) -> str:
        return f"<Listing id={self.id} status={self.status!r} price={self.price_cents}>"


class Purchase(Base):
    __tablename__ = "purchases"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=uuid.uuid4
    )
    buyer_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True
    )
    listing_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("marketplace_listings.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    # Stripe Checkout Session id; NULL for free grants.
    stripe_checkout_id: Mapped[str | None] = mapped_column(
        Text, unique=True, nullable=True
    )
    amount_cents: Mapped[int] = mapped_column(Integer, nullable=False)
    fee_cents: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    # pending / completed / refunded
    status: Mapped[str] = mapped_column(
        String(16), nullable=False, default="pending", server_default="pending"
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )

    def __repr__(self) -> str:
        return f"<Purchase id={self.id} status={self.status!r}>"


class Entitlement(Base):
    __tablename__ = "entitlements"

    buyer_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("users.id", ondelete="CASCADE"), primary_key=True
    )
    pack_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("packs.id", ondelete="CASCADE"), primary_key=True
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )

    pack: Mapped["Pack"] = relationship()

    def __repr__(self) -> str:
        return f"<Entitlement buyer={self.buyer_id} pack={self.pack_id}>"

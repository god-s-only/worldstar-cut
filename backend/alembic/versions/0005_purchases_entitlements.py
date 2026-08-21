"""purchases + entitlements

Revision ID: 0005
Revises: 0004
Create Date: 2026-08-21

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


revision: str = "0005"
down_revision: Union[str, None] = "0004"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "purchases",
        sa.Column("id", sa.Uuid(), nullable=False),
        sa.Column("buyer_id", sa.Uuid(), nullable=False),
        sa.Column("listing_id", sa.Uuid(), nullable=False),
        sa.Column("stripe_checkout_id", sa.Text(), nullable=True),
        sa.Column("amount_cents", sa.Integer(), nullable=False),
        sa.Column("fee_cents", sa.Integer(), server_default="0", nullable=False),
        sa.Column(
            "status", sa.String(length=16), server_default="pending", nullable=False
        ),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("now()"),
            nullable=False,
        ),
        sa.ForeignKeyConstraint(
            ["buyer_id"],
            ["users.id"],
            name=op.f("fk_purchases_buyer_id_users"),
            ondelete="CASCADE",
        ),
        sa.ForeignKeyConstraint(
            ["listing_id"],
            ["marketplace_listings.id"],
            name=op.f("fk_purchases_listing_id_marketplace_listings"),
            ondelete="CASCADE",
        ),
        sa.PrimaryKeyConstraint("id", name=op.f("pk_purchases")),
        sa.UniqueConstraint(
            "stripe_checkout_id", name=op.f("uq_purchases_stripe_checkout_id")
        ),
    )
    op.create_index(op.f("ix_purchases_buyer_id"), "purchases", ["buyer_id"])
    op.create_index(op.f("ix_purchases_listing_id"), "purchases", ["listing_id"])

    op.create_table(
        "entitlements",
        sa.Column("buyer_id", sa.Uuid(), nullable=False),
        sa.Column("pack_id", sa.Uuid(), nullable=False),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("now()"),
            nullable=False,
        ),
        sa.ForeignKeyConstraint(
            ["buyer_id"],
            ["users.id"],
            name=op.f("fk_entitlements_buyer_id_users"),
            ondelete="CASCADE",
        ),
        sa.ForeignKeyConstraint(
            ["pack_id"],
            ["packs.id"],
            name=op.f("fk_entitlements_pack_id_packs"),
            ondelete="CASCADE",
        ),
        sa.PrimaryKeyConstraint("buyer_id", "pack_id", name=op.f("pk_entitlements")),
    )


def downgrade() -> None:
    op.drop_table("entitlements")
    op.drop_index(op.f("ix_purchases_listing_id"), table_name="purchases")
    op.drop_index(op.f("ix_purchases_buyer_id"), table_name="purchases")
    op.drop_table("purchases")

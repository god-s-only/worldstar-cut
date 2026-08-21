"""marketplace listings + packs full-text search vector

Revision ID: 0003
Revises: 0002
Create Date: 2026-08-21

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql


revision: str = "0003"
down_revision: Union[str, None] = "0002"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "marketplace_listings",
        sa.Column("id", sa.Uuid(), nullable=False),
        sa.Column("pack_id", sa.Uuid(), nullable=False),
        sa.Column("seller_id", sa.Uuid(), nullable=False),
        sa.Column("price_cents", sa.Integer(), nullable=False),
        sa.Column(
            "currency", sa.String(length=8), server_default="usd", nullable=False
        ),
        sa.Column(
            "status", sa.String(length=16), server_default="pending", nullable=False
        ),
        sa.Column("reject_reason", sa.Text(), nullable=True),
        sa.Column("downloads", sa.Integer(), server_default="0", nullable=False),
        sa.Column("rating_avg", sa.Double(), nullable=True),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("now()"),
            nullable=False,
        ),
        sa.ForeignKeyConstraint(
            ["pack_id"],
            ["packs.id"],
            name=op.f("fk_marketplace_listings_pack_id_packs"),
            ondelete="CASCADE",
        ),
        sa.ForeignKeyConstraint(
            ["seller_id"],
            ["users.id"],
            name=op.f("fk_marketplace_listings_seller_id_users"),
            ondelete="CASCADE",
        ),
        sa.PrimaryKeyConstraint("id", name=op.f("pk_marketplace_listings")),
        sa.UniqueConstraint(
            "pack_id", name=op.f("uq_marketplace_listings_pack_id")
        ),
    )
    op.create_index(
        op.f("ix_marketplace_listings_seller_id"),
        "marketplace_listings",
        ["seller_id"],
        unique=False,
    )
    op.create_index(
        op.f("ix_marketplace_listings_status"),
        "marketplace_listings",
        ["status"],
        unique=False,
    )

    op.add_column(
        "packs",
        sa.Column(
            "search_vector",
            postgresql.TSVECTOR(),
            sa.Computed(
                "to_tsvector('english', coalesce(title, '') || ' ' || coalesce(description, ''))",
                persisted=True,
            ),
            nullable=True,
        ),
    )
    op.create_index(
        "ix_packs_search_vector",
        "packs",
        ["search_vector"],
        unique=False,
        postgresql_using="gin",
    )


def downgrade() -> None:
    op.drop_index("ix_packs_search_vector", table_name="packs")
    op.drop_column("packs", "search_vector")
    op.drop_index(
        op.f("ix_marketplace_listings_status"), table_name="marketplace_listings"
    )
    op.drop_index(
        op.f("ix_marketplace_listings_seller_id"), table_name="marketplace_listings"
    )
    op.drop_table("marketplace_listings")

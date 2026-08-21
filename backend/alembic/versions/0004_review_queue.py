"""review queue for moderation

Revision ID: 0004
Revises: 0003
Create Date: 2026-08-21

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql


revision: str = "0004"
down_revision: Union[str, None] = "0003"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "review_queue",
        sa.Column("id", sa.Uuid(), nullable=False),
        sa.Column("listing_id", sa.Uuid(), nullable=False),
        sa.Column("vision_score", postgresql.JSONB(), nullable=True),
        sa.Column(
            "status", sa.String(length=16), server_default="pending", nullable=False
        ),
        sa.Column("reviewer_id", sa.Uuid(), nullable=True),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("now()"),
            nullable=False,
        ),
        sa.Column("decided_at", sa.DateTime(timezone=True), nullable=True),
        sa.ForeignKeyConstraint(
            ["listing_id"],
            ["marketplace_listings.id"],
            name=op.f("fk_review_queue_listing_id_marketplace_listings"),
            ondelete="CASCADE",
        ),
        sa.ForeignKeyConstraint(
            ["reviewer_id"],
            ["users.id"],
            name=op.f("fk_review_queue_reviewer_id_users"),
            ondelete="SET NULL",
        ),
        sa.PrimaryKeyConstraint("id", name=op.f("pk_review_queue")),
    )
    op.create_index(
        op.f("ix_review_queue_listing_id"),
        "review_queue",
        ["listing_id"],
        unique=False,
    )
    op.create_index(
        op.f("ix_review_queue_status"), "review_queue", ["status"], unique=False
    )


def downgrade() -> None:
    op.drop_index(op.f("ix_review_queue_status"), table_name="review_queue")
    op.drop_index(op.f("ix_review_queue_listing_id"), table_name="review_queue")
    op.drop_table("review_queue")

"""users.is_admin + payouts

Revision ID: 0006
Revises: 0005
Create Date: 2026-08-21

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


revision: str = "0006"
down_revision: Union[str, None] = "0005"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column(
        "users",
        sa.Column("is_admin", sa.Boolean(), server_default="false", nullable=False),
    )
    op.create_table(
        "payouts",
        sa.Column("id", sa.Uuid(), nullable=False),
        sa.Column("seller_id", sa.Uuid(), nullable=False),
        sa.Column("amount_cents", sa.Integer(), nullable=False),
        sa.Column(
            "currency", sa.String(length=8), server_default="usd", nullable=False
        ),
        sa.Column("stripe_transfer_id", sa.Text(), nullable=True),
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
            ["seller_id"],
            ["users.id"],
            name=op.f("fk_payouts_seller_id_users"),
            ondelete="CASCADE",
        ),
        sa.PrimaryKeyConstraint("id", name=op.f("pk_payouts")),
        sa.UniqueConstraint(
            "stripe_transfer_id", name=op.f("uq_payouts_stripe_transfer_id")
        ),
    )
    op.create_index(op.f("ix_payouts_seller_id"), "payouts", ["seller_id"])


def downgrade() -> None:
    op.drop_index(op.f("ix_payouts_seller_id"), table_name="payouts")
    op.drop_table("payouts")
    op.drop_column("users", "is_admin")

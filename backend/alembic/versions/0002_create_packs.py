"""create packs, pack_items, tags, pack_tags

Revision ID: 0002
Revises: 0001
Create Date: 2026-08-21

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


revision: str = "0002"
down_revision: Union[str, None] = "0001"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "tags",
        sa.Column("id", sa.Integer(), autoincrement=True, nullable=False),
        sa.Column("name", sa.String(length=64), nullable=False),
        sa.PrimaryKeyConstraint("id", name=op.f("pk_tags")),
        sa.UniqueConstraint("name", name=op.f("uq_tags_name")),
    )
    op.create_table(
        "packs",
        sa.Column("id", sa.Uuid(), nullable=False),
        sa.Column("owner_id", sa.Uuid(), nullable=False),
        sa.Column("title", sa.String(length=120), nullable=False),
        sa.Column("description", sa.Text(), nullable=True),
        sa.Column("cover_url", sa.Text(), nullable=True),
        sa.Column("is_public", sa.Boolean(), server_default="false", nullable=False),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("now()"),
            nullable=False,
        ),
        sa.Column(
            "updated_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("now()"),
            nullable=False,
        ),
        sa.ForeignKeyConstraint(
            ["owner_id"],
            ["users.id"],
            name=op.f("fk_packs_owner_id_users"),
            ondelete="CASCADE",
        ),
        sa.PrimaryKeyConstraint("id", name=op.f("pk_packs")),
    )
    op.create_index(op.f("ix_packs_owner_id"), "packs", ["owner_id"], unique=False)
    op.create_table(
        "pack_items",
        sa.Column("id", sa.Uuid(), nullable=False),
        sa.Column("pack_id", sa.Uuid(), nullable=False),
        sa.Column("type", sa.String(length=16), nullable=False),
        sa.Column("file_url", sa.Text(), nullable=False),
        sa.Column("preview_url", sa.Text(), nullable=True),
        sa.Column("width", sa.Integer(), nullable=True),
        sa.Column("height", sa.Integer(), nullable=True),
        sa.Column("duration_ms", sa.Integer(), nullable=True),
        sa.Column("position", sa.Integer(), nullable=False),
        sa.ForeignKeyConstraint(
            ["pack_id"],
            ["packs.id"],
            name=op.f("fk_pack_items_pack_id_packs"),
            ondelete="CASCADE",
        ),
        sa.PrimaryKeyConstraint("id", name=op.f("pk_pack_items")),
    )
    op.create_index(
        op.f("ix_pack_items_pack_id"), "pack_items", ["pack_id"], unique=False
    )
    op.create_table(
        "pack_tags",
        sa.Column("pack_id", sa.Uuid(), nullable=False),
        sa.Column("tag_id", sa.Integer(), nullable=False),
        sa.ForeignKeyConstraint(
            ["pack_id"],
            ["packs.id"],
            name=op.f("fk_pack_tags_pack_id_packs"),
            ondelete="CASCADE",
        ),
        sa.ForeignKeyConstraint(
            ["tag_id"],
            ["tags.id"],
            name=op.f("fk_pack_tags_tag_id_tags"),
            ondelete="CASCADE",
        ),
        sa.PrimaryKeyConstraint("pack_id", "tag_id", name=op.f("pk_pack_tags")),
    )


def downgrade() -> None:
    op.drop_table("pack_tags")
    op.drop_index(op.f("ix_pack_items_pack_id"), table_name="pack_items")
    op.drop_table("pack_items")
    op.drop_index(op.f("ix_packs_owner_id"), table_name="packs")
    op.drop_table("packs")
    op.drop_table("tags")

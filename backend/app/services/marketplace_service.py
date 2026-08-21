"""Marketplace queries: browse/search approved listings (plan §5, §8)."""
from fastapi import HTTPException
from sqlalchemy import ColumnElement, and_, func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.marketplace import Listing
from app.models.pack import Pack, PackItem, Tag, pack_tags

SORT_NEWEST = "newest"
SORT_PRICE_ASC = "price_asc"
SORT_PRICE_DESC = "price_desc"
SORT_POPULAR = "popular"
VALID_SORTS = {SORT_NEWEST, SORT_PRICE_ASC, SORT_PRICE_DESC, SORT_POPULAR}

DEFAULT_PAGE_SIZE = 20
MAX_PAGE_SIZE = 100


def parse_sort(sort: str | None) -> str:
    if sort is None:
        return SORT_NEWEST
    if sort not in VALID_SORTS:
        raise HTTPException(
            422, f"Invalid sort {sort!r}; allowed: {', '.join(sorted(VALID_SORTS))}"
        )
    return sort


def order_clause(sort: str):
    return {
        SORT_NEWEST: Listing.created_at.desc(),
        SORT_PRICE_ASC: Listing.price_cents.asc(),
        SORT_PRICE_DESC: Listing.price_cents.desc(),
        SORT_POPULAR: Listing.downloads.desc(),
    }[sort]


async def item_counts(db: AsyncSession, pack_ids: list) -> dict:
    if not pack_ids:
        return {}
    rows = await db.execute(
        select(PackItem.pack_id, func.count())
        .where(PackItem.pack_id.in_(pack_ids))
        .group_by(PackItem.pack_id)
    )
    return dict(rows.all())


async def search_listings(
    db: AsyncSession,
    *,
    q: str | None = None,
    tag: str | None = None,
    sort: str = SORT_NEWEST,
    page: int = 1,
    page_size: int = DEFAULT_PAGE_SIZE,
) -> tuple[list[Listing], int]:
    conditions: list[ColumnElement[bool]] = [Listing.status == "approved"]

    if q:
        conditions.append(
            Pack.search_vector.op("@@")(
                func.plainto_tsquery("english", q)
            )
        )
    if tag:
        tag_subq = (
            select(pack_tags.c.pack_id)
            .join(Tag, Tag.id == pack_tags.c.tag_id)
            .where(Tag.name == tag.strip().lower())
        )
        conditions.append(Pack.id.in_(tag_subq))

    where = conditions[0] if len(conditions) == 1 else and_(*conditions)

    total = await db.scalar(
        select(func.count())
        .select_from(Listing)
        .join(Pack, Listing.pack_id == Pack.id)
        .where(where)
    )

    listings = (
        (
            await db.scalars(
                select(Listing)
                .join(Pack, Listing.pack_id == Pack.id)
                .where(where)
                .order_by(order_clause(sort), Listing.created_at.desc())
                .offset((page - 1) * page_size)
                .limit(page_size)
            )
        )
        .unique()
        .all()
    )
    return list(listings), int(total or 0)

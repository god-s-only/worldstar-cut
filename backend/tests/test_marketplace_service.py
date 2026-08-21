import pytest
from pydantic import ValidationError

from app.schemas.marketplace import ListingCreate
from app.services import marketplace_service as ms


def test_parse_sort_defaults_to_newest():
    assert ms.parse_sort(None) == "newest"


def test_parse_sort_accepts_all_valid():
    for s in ("newest", "price_asc", "price_desc", "popular"):
        assert ms.parse_sort(s) == s


def test_parse_sort_rejects_unknown():
    with pytest.raises(Exception):
        ms.parse_sort("hacker-sort")


def test_listing_create_rejects_negative_price():
    with pytest.raises(ValidationError):
        ListingCreate(
            pack_id="00000000-0000-0000-0000-000000000001",
            price_cents=-1,
        )


def test_listing_create_defaults():
    lc = ListingCreate(
        pack_id="00000000-0000-0000-0000-000000000001",
        price_cents=499,
    )
    assert lc.currency == "usd"
    assert lc.price_cents == 499


def test_listing_create_rejects_bad_currency():
    with pytest.raises(ValidationError):
        ListingCreate(
            pack_id="00000000-0000-0000-0000-000000000001",
            price_cents=100,
            currency="USDD",
        )

import io
import json

import pytest
from fastapi import HTTPException
from PIL import Image

from app.services.pack_service import (
    MAX_ITEM_BYTES,
    generate_preview,
    normalize_tags,
    pack_object_key,
    preview_object_key,
    sniff_item,
)


def _png_bytes(width=800, height=600) -> bytes:
    img = Image.new("RGBA", (width, height), (255, 0, 0, 255))
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    return buf.getvalue()


def test_sniff_accepts_png_sticker():
    sniffed = sniff_item(_png_bytes(), "cool-sticker.PNG")
    assert sniffed.ext == "png"
    assert sniffed.content_type == "image/png"
    assert sniffed.item_type == "sticker"


def test_sniff_accepts_lottie_json():
    lottie = json.dumps({"v": "5.7.4", "layers": [], "assets": []}).encode()
    sniffed = sniff_item(lottie, "anim.json")
    assert sniffed.item_type == "animation"
    assert sniffed.content_type == "application/json"


def test_sniff_rejects_unknown_extension():
    with pytest.raises(HTTPException) as exc:
        sniff_item(b"data", "virus.exe")
    assert exc.value.status_code == 422


def test_sniff_rejects_oversize():
    big = b"x" * (MAX_ITEM_BYTES + 1)
    with pytest.raises(HTTPException) as exc:
        sniff_item(big, "big.png")
    assert exc.value.status_code == 422


def test_sniff_rejects_empty():
    with pytest.raises(HTTPException):
        sniff_item(b"", "empty.png")


def test_sniff_rejects_non_lottie_json():
    with pytest.raises(HTTPException):
        sniff_item(b'{"not": "lottie"}', "fake.json")


def test_generate_preview_scales_to_webp():
    preview = generate_preview(_png_bytes(800, 600))
    assert preview is not None
    with Image.open(io.BytesIO(preview)) as img:
        assert img.format == "WEBP"
        assert max(img.size) <= 512


def test_generate_preview_returns_none_for_json():
    lottie = json.dumps({"layers": []}).encode()
    assert generate_preview(lottie) is None


def test_normalize_tags_lowercases_and_dedupes():
    assert normalize_tags(["Glitch", " glitch ", "Anime"]) == ["glitch", "anime"]


def test_normalize_tags_rejects_too_many():
    with pytest.raises(HTTPException):
        normalize_tags([f"tag{i}" for i in range(12)])


def test_object_keys_are_unique_and_scoped():
    k1 = pack_object_key("00000000-0000-0000-0000-000000000001", "png")
    k2 = pack_object_key("00000000-0000-0000-0000-000000000001", "png")
    assert k1.startswith("00000000-0000-0000-0000-000000000001/")
    assert k1.endswith(".png")
    assert k1 != k2
    assert preview_object_key(
        "00000000-0000-0000-0000-000000000001"
    ).endswith(".webp")

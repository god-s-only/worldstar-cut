"""Pack domain logic: item validation, preview generation, tag normalization."""
import io
import json
import uuid
from dataclasses import dataclass

from fastapi import HTTPException
from PIL import Image

from app.core.config import settings

MAX_ITEMS_PER_PACK = 30
MAX_ITEM_BYTES = 2 * 1024 * 1024  # plan: each item < 2MB
MAX_TAGS_PER_PACK = 10
PREVIEW_MAX_SIZE = (512, 512)
PREVIEW_FORMAT = "WEBP"

# extension -> (content_type, item_type)
ALLOWED_TYPES: dict[str, tuple[str, str]] = {
    ".png": ("image/png", "sticker"),
    ".webp": ("image/webp", "sticker"),
    ".json": ("application/json", "animation"),
}


@dataclass(frozen=True)
class SniffedItem:
    ext: str
    content_type: str
    item_type: str


def sniff_item(data: bytes, filename: str) -> SniffedItem:
    """Validate raw bytes + filename against pack item rules (plan §4)."""
    if len(data) == 0:
        raise HTTPException(422, "Empty file")
    if len(data) > MAX_ITEM_BYTES:
        raise HTTPException(422, "File exceeds the 2MB limit per item")

    dot = filename.rfind(".")
    ext = filename[dot:].lower() if dot != -1 else ""
    if ext not in ALLOWED_TYPES:
        allowed = ", ".join(sorted(ALLOWED_TYPES))
        raise HTTPException(422, f"Unsupported file type {ext or '(none)'}; allowed: {allowed}")

    if ext == ".json":
        try:
            doc = json.loads(data.decode("utf-8"))
        except (UnicodeDecodeError, json.JSONDecodeError):
            raise HTTPException(422, "Invalid JSON animation file")
        if not isinstance(doc, dict) or "layers" not in doc:
            raise HTTPException(422, "JSON does not look like a Lottie animation")

    content_type, item_type = ALLOWED_TYPES[ext]
    return SniffedItem(ext=ext.lstrip("."), content_type=content_type, item_type=item_type)


def generate_preview(data: bytes) -> bytes | None:
    """512x512-max webp preview for raster items; None for non-images."""
    try:
        with Image.open(io.BytesIO(data)) as img:
            img = img.convert("RGBA")
            img.thumbnail(PREVIEW_MAX_SIZE)
            buf = io.BytesIO()
            img.save(buf, format=PREVIEW_FORMAT, quality=85)
            return buf.getvalue()
    except Exception:  # noqa: BLE001 - previews are best-effort
        return None


def normalize_tags(names: list[str]) -> list[str]:
    cleaned: list[str] = []
    for raw in names:
        tag = raw.strip().lower()
        if not tag or len(tag) > 64:
            raise HTTPException(422, f"Invalid tag: {raw!r}")
        if tag not in cleaned:
            cleaned.append(tag)
    if len(cleaned) > MAX_TAGS_PER_PACK:
        raise HTTPException(422, f"Max {MAX_TAGS_PER_PACK} tags per pack")
    return cleaned


def pack_object_key(pack_id: uuid.UUID, ext: str) -> str:
    return f"{pack_id}/{uuid.uuid4().hex}.{ext}"


def preview_object_key(pack_id: uuid.UUID) -> str:
    return f"{pack_id}/{uuid.uuid4().hex}.webp"


def packs_bucket() -> str:
    return settings.OBJECT_STORAGE_BUCKET_PACKS


def previews_bucket() -> str:
    return settings.OBJECT_STORAGE_BUCKET_PREVIEWS

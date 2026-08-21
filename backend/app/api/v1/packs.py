import io
import uuid

from fastapi import APIRouter, Depends, File, Form, HTTPException, UploadFile, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.core.deps import get_current_user
from app.db.session import get_db
from app.models.pack import Pack, PackItem, Tag
from app.models.user import User
from app.schemas.pack import PackCreate, PackItemOut, PackOut, PackUpdate
from app.services import pack_service, storage

router = APIRouter(prefix="/packs", tags=["packs"])


def _item_out(item: PackItem) -> PackItemOut:
    return PackItemOut(
        id=item.id,
        type=item.type,
        file_url=storage.presigned_get(item.file_url),
        preview_url=(
            storage.presigned_get(item.preview_url, bucket=storage.previews_bucket())
            if item.preview_url
            else None
        ),
        width=item.width,
        height=item.height,
        duration_ms=item.duration_ms,
        position=item.position,
    )


def _pack_out(pack: Pack) -> PackOut:
    return PackOut(
        id=pack.id,
        owner_id=pack.owner_id,
        title=pack.title,
        description=pack.description,
        cover_url=pack.cover_url,
        is_public=pack.is_public,
        tags=[t.name for t in pack.tags],
        created_at=pack.created_at,
        updated_at=pack.updated_at,
        items=[_item_out(i) for i in pack.items],
    )


async def _get_owned_pack(
    pack_id: uuid.UUID, user: User, db: AsyncSession
) -> Pack:
    """Fetch pack with items+tags; 404 unless it exists and is readable
    (owner or public). Callers enforce ownership for mutations."""
    pack = await db.scalar(
        select(Pack)
        .options(selectinload(Pack.items), selectinload(Pack.tags))
        .where(Pack.id == pack_id)
    )
    if pack is None or (pack.owner_id != user.id and not pack.is_public):
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Pack not found")
    return pack


async def _resolve_tags(db: AsyncSession, names: list[str]) -> list[Tag]:
    normalized = pack_service.normalize_tags(names)
    if not normalized:
        return []
    existing = (
        (await db.scalars(select(Tag).where(Tag.name.in_(normalized)))).all()
    )
    found = {t.name: t for t in existing}
    for name in normalized:
        if name not in found:
            tag = Tag(name=name)
            db.add(tag)
            found[name] = tag
    return list(found.values())


@router.post("", response_model=PackOut, status_code=status.HTTP_201_CREATED)
async def create_pack(
    payload: PackCreate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> PackOut:
    pack = Pack(
        owner_id=current_user.id,
        title=payload.title,
        description=payload.description,
        is_public=payload.is_public,
    )
    pack.tags = await _resolve_tags(db, payload.tags)
    db.add(pack)
    await db.commit()
    await db.refresh(pack)
    return _pack_out(pack)


@router.get("/me", response_model=list[PackOut])
async def my_packs(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> list[PackOut]:
    packs = (
        (
            await db.scalars(
                select(Pack)
                .options(selectinload(Pack.items), selectinload(Pack.tags))
                .where(Pack.owner_id == current_user.id)
                .order_by(Pack.created_at.desc())
            )
        )
        .all()
    )
    return [_pack_out(p) for p in packs]


@router.get("/{pack_id}", response_model=PackOut)
async def get_pack(
    pack_id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> PackOut:
    pack = await _get_owned_pack(pack_id, current_user, db)
    return _pack_out(pack)


@router.put("/{pack_id}", response_model=PackOut)
async def update_pack(
    pack_id: uuid.UUID,
    payload: PackUpdate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> PackOut:
    pack = await _get_owned_pack(pack_id, current_user, db)
    if pack.owner_id != current_user.id:
        raise HTTPException(status.HTTP_403_FORBIDDEN, "Not the pack owner")

    if payload.title is not None:
        pack.title = payload.title
    if payload.description is not None:
        pack.description = payload.description
    if payload.is_public is not None:
        pack.is_public = payload.is_public
    if payload.tags is not None:
        pack.tags = await _resolve_tags(db, payload.tags)

    await db.commit()
    await db.refresh(pack)
    return _pack_out(pack)


@router.delete("/{pack_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_pack(
    pack_id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> None:
    pack = await _get_owned_pack(pack_id, current_user, db)
    if pack.owner_id != current_user.id:
        raise HTTPException(status.HTTP_403_FORBIDDEN, "Not the pack owner")

    item_keys = [(i.file_url, i.preview_url) for i in pack.items]
    await db.delete(pack)
    await db.commit()

    # Best-effort object cleanup after DB success.
    for file_key, preview_key in item_keys:
        storage.delete_object(file_key)
        if preview_key:
            storage.delete_object(preview_key, bucket=storage.previews_bucket())


@router.post(
    "/{pack_id}/items",
    response_model=PackItemOut,
    status_code=status.HTTP_201_CREATED,
)
async def add_pack_item(
    pack_id: uuid.UUID,
    file: UploadFile = File(...),
    width: int | None = Form(default=None),
    height: int | None = Form(default=None),
    duration_ms: int | None = Form(default=None),
    position: int | None = Form(default=None),
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> PackItemOut:
    pack = await _get_owned_pack(pack_id, current_user, db)
    if pack.owner_id != current_user.id:
        raise HTTPException(status.HTTP_403_FORBIDDEN, "Not the pack owner")
    if len(pack.items) >= pack_service.MAX_ITEMS_PER_PACK:
        raise HTTPException(
            status.HTTP_422_UNPROCESSABLE_ENTITY,
            f"Pack is full ({pack_service.MAX_ITEMS_PER_PACK} items max)",
        )

    data = await file.read()
    sniffed = pack_service.sniff_item(data, file.filename or "")
    await file.close()

    file_key = pack_service.pack_object_key(pack.id, sniffed.ext)
    storage.upload_file(file_key, io.BytesIO(data), sniffed.content_type)

    preview_key: str | None = None
    if sniffed.item_type == "sticker":
        preview_data = pack_service.generate_preview(data)
        if preview_data:
            preview_key = pack_service.preview_object_key(pack.id)
            storage.upload_file(
                preview_key,
                io.BytesIO(preview_data),
                "image/webp",
                bucket=storage.previews_bucket(),
            )

    item = PackItem(
        pack_id=pack.id,
        type=sniffed.item_type,
        file_url=file_key,
        preview_url=preview_key,
        width=width,
        height=height,
        duration_ms=duration_ms,
        position=len(pack.items) if position is None else position,
    )
    db.add(item)
    await db.commit()
    await db.refresh(item)
    return _item_out(item)

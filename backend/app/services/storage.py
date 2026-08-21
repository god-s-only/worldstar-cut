"""Object storage access via the S3-compatible API of OCI Object Storage.

Works against:
- OCI Object Storage: endpoint https://<namespace>.compat.objectstorage.<region>.oraclecloud.com
  auth via OCI Customer Secret Keys (access key / secret)
- MinIO for local development
"""
import logging
from functools import lru_cache
from typing import BinaryIO

import boto3
from botocore.client import Config

from app.core.config import settings

logger = logging.getLogger(__name__)

DEFAULT_EXPIRES_SECONDS = 3600


@lru_cache(maxsize=1)
def get_object_storage_client():
    kwargs: dict = {
        "region_name": settings.OBJECT_STORAGE_REGION,
        "config": Config(signature_version="s3v4", retries={"max_attempts": 3}),
    }
    if settings.OBJECT_STORAGE_ACCESS_KEY and settings.OBJECT_STORAGE_SECRET_KEY:
        kwargs["aws_access_key_id"] = settings.OBJECT_STORAGE_ACCESS_KEY
        kwargs["aws_secret_access_key"] = settings.OBJECT_STORAGE_SECRET_KEY
    if settings.OBJECT_STORAGE_ENDPOINT_URL:
        kwargs["endpoint_url"] = settings.OBJECT_STORAGE_ENDPOINT_URL
    return boto3.client("s3", **kwargs)


def upload_file(
    key: str,
    fileobj: BinaryIO,
    content_type: str,
    bucket: str | None = None,
) -> None:
    """Upload with immutable-style caching (keys are unique UUIDs)."""
    get_object_storage_client().upload_fileobj(
        fileobj,
        bucket or settings.OBJECT_STORAGE_BUCKET_PACKS,
        key,
        ExtraArgs={
            "ContentType": content_type,
            "CacheControl": "max-age=31536000",
        },
    )


def presigned_get(
    key: str,
    expires_in: int = DEFAULT_EXPIRES_SECONDS,
    bucket: str | None = None,
) -> str | None:
    """Presigned GET URL; None when storage is unreachable/misconfigured."""
    try:
        return get_object_storage_client().generate_presigned_url(
            "get_object",
            Params={
                "Bucket": bucket or settings.OBJECT_STORAGE_BUCKET_PACKS,
                "Key": key,
            },
            ExpiresIn=expires_in,
        )
    except Exception:  # noqa: BLE001 - degrade gracefully in dev w/o storage
        logger.exception("Failed to presign GET for key=%s", key)
        return None


def delete_object(key: str, bucket: str | None = None) -> None:
    try:
        get_object_storage_client().delete_object(
            Bucket=bucket or settings.OBJECT_STORAGE_BUCKET_PACKS, Key=key
        )
    except Exception:  # noqa: BLE001 - best-effort cleanup
        logger.exception("Failed to delete object key=%s", key)


def get_object_bytes(key: str, bucket: str | None = None) -> bytes | None:
    """Download an object fully; None when storage is unreachable."""
    try:
        response = get_object_storage_client().get_object(
            Bucket=bucket or settings.OBJECT_STORAGE_BUCKET_PACKS, Key=key
        )
        return response["Body"].read()
    except Exception:  # noqa: BLE001
        logger.exception("Failed to read object key=%s", key)
        return None

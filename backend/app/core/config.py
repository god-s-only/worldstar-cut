from typing import List

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    PROJECT_NAME: str = "WorldstarCut API"
    VERSION: str = "0.1.0"
    API_V1_STR: str = "/api/v1"

    # CORS
    BACKEND_CORS_ORIGINS: List[str] = ["*"]

    # DB — postgres (update .env)
    DATABASE_URL: str = "postgresql+asyncpg://postgres:postgres@localhost:5432/worldstarcut"
    DATABASE_URL_SYNC: str = "postgresql://postgres:postgres@localhost:5432/worldstarcut"

    # Redis
    REDIS_URL: str = "redis://localhost:6379/0"

    # JWT
    SECRET_KEY: str = "change-me-in-env"
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60 * 24 * 7  # 7 days

    # Storage — OCI Object Storage (S3-compatible API; MinIO for local dev)
    OBJECT_STORAGE_ENDPOINT_URL: str = ""  # e.g. https://<namespace>.compat.objectstorage.me-jeddah-1.oraclecloud.com or http://localhost:9000
    OBJECT_STORAGE_REGION: str = "me-jeddah-1"
    OBJECT_STORAGE_ACCESS_KEY: str = ""  # OCI Customer Secret Key — Access Key
    OBJECT_STORAGE_SECRET_KEY: str = ""  # OCI Customer Secret Key — Secret
    OBJECT_STORAGE_BUCKET_PACKS: str = "wsc-packs"
    OBJECT_STORAGE_BUCKET_PREVIEWS: str = "wsc-previews"
    OBJECT_STORAGE_BUCKET_EXPORTS: str = "wsc-exports"

    # Moderation (OCI Vision; manual review when unavailable)
    OCI_NAMESPACE: str = ""
    MODERATION_AUTO_APPROVE_BELOW: float = 0.2
    MODERATION_REJECT_ABOVE: float = 0.8
    MODERATION_BLOCKED_LABELS: str = (
        "explicit,nude,nudity,porn,pornographic,sexual,suggestive"
    )

    # Stripe
    STRIPE_SECRET_KEY: str = ""
    STRIPE_WEBHOOK_SECRET: str = ""
    STRIPE_PUBLISHABLE_KEY: str = ""
    STRIPE_PLATFORM_FEE_PERCENT: float = 10.0

    # Public base URL used for Stripe redirects
    APP_BASE_URL: str = "http://localhost:8000"

    model_config = SettingsConfigDict(env_file=".env", case_sensitive=True)


settings = Settings()

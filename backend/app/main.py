from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.core.config import settings
from app.api.v1.auth import router as auth_router
from app.api.v1.health import router as health_router
from app.api.v1.marketplace import router as marketplace_router
from app.api.v1.packs import router as packs_router
from app.api.v1.webhooks.stripe import router as stripe_webhook_router

app = FastAPI(
    title=settings.PROJECT_NAME,
    version=settings.VERSION,
    description="WorldstarCut Marketplace & Backend API",
)

# CORS — allow Android app and web
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.BACKEND_CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Routers
app.include_router(health_router, prefix="/api/v1", tags=["health"])
app.include_router(auth_router, prefix="/api/v1")
app.include_router(packs_router, prefix="/api/v1")
app.include_router(marketplace_router, prefix="/api/v1")
app.include_router(stripe_webhook_router)


@app.get("/", tags=["root"])
async def root():
    return {
        "name": settings.PROJECT_NAME,
        "version": settings.VERSION,
        "status": "ok",
        "docs": "/docs",
    }


@app.get("/health", tags=["health"])
async def health():
    return {"status": "ok"}

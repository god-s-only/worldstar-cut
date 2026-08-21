from fastapi import APIRouter

router = APIRouter()


@router.get("/health", tags=["health"])
async def health_check():
    return {"status": "ok", "service": "worldstarcut-api"}


@router.get("/ready", tags=["health"])
async def readiness():
    # TODO: check DB / Redis connectivity
    return {"status": "ready"}

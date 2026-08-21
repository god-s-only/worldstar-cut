"""Moderation: OCI Vision NSFW/label check + decision policy (plan §6).

The `oci` SDK is imported lazily; without credentials the service degrades to
"manual" decisions so listings simply wait for human review.
"""
import base64
import logging

from app.core.config import settings

logger = logging.getLogger(__name__)

ACTION_APPROVE = "approve"
ACTION_REJECT = "reject"
ACTION_MANUAL = "manual"

DEFAULT_BLOCKED_LABELS = "explicit,nude,nudity,porn,pornographic,sexual,suggestive"


def decide_action(score: float | None) -> str:
    """Policy: high score auto-reject, low score auto-approve, else manual."""
    if score is None:
        return ACTION_MANUAL
    if score >= settings.MODERATION_REJECT_ABOVE:
        return ACTION_REJECT
    if score < settings.MODERATION_AUTO_APPROVE_BELOW:
        return ACTION_APPROVE
    return ACTION_MANUAL


def _blocked_labels() -> list[str]:
    raw = settings.MODERATION_BLOCKED_LABELS or DEFAULT_BLOCKED_LABELS
    return [x.strip().lower() for x in raw.split(",") if x.strip()]


def _vision_client():
    try:
        import oci
    except ImportError:
        logger.warning("oci SDK not installed; moderation runs in manual mode")
        return None
    try:
        config = oci.config.from_file()
        oci.config.validate_config(config)
        return oci.ai_vision.AIServiceVisionClient(config)
    except Exception:  # noqa: BLE001 - any config problem => manual mode
        logger.exception("OCI Vision unavailable; moderation runs in manual mode")
        return None


def check_image(data: bytes) -> dict:
    """Classify image; returns {"score": float|None, "labels": [...], "provider": str}.

    score = highest confidence among blocked labels (0.0 when none matched),
    None when no provider is available.
    """
    client = _vision_client()
    if client is None:
        return {"score": None, "labels": [], "provider": "none"}

    try:
        import oci

        response = client.analyze_image(
            analyze_image_details=oci.ai_vision.models.AnalyzeImageDetails(
                features=[
                    oci.ai_vision.models.ImageClassificationFeature(
                        model_type="IMAGE_CLASSIFICATION", max_results=10
                    )
                ],
                image=oci.ai_vision.models.InlineImageDetails(
                    source="INLINE",
                    data=base64.b64encode(data).decode("ascii"),
                ),
            )
        )
    except Exception:  # noqa: BLE001 - provider failure => manual review
        logger.exception("OCI Vision call failed")
        return {"score": None, "labels": [], "provider": "error"}

    labels = [
        {"name": l.name.lower(), "confidence": round(l.confidence, 4)}
        for l in (response.data.labels or [])
    ]
    blocked = set(_blocked_labels())
    matches = [l["confidence"] for l in labels if l["name"] in blocked]
    score = max(matches) if matches else (0.0 if labels else None)
    return {"score": score, "labels": labels, "provider": "oci-vision"}

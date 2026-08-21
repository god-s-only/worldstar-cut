import pytest

from app.services import moderation_service as mod


def test_decide_rejects_high_score():
    assert mod.decide_action(0.95) == "reject"


def test_decide_approves_low_score():
    assert mod.decide_action(0.05) == "approve"


def test_decide_manual_in_gray_zone():
    assert mod.decide_action(0.5) == "manual"


def test_decide_manual_when_no_provider():
    assert mod.decide_action(None) == "manual"


def test_check_image_degrades_to_manual_without_oci():
    result = mod.check_image(b"fake-image-bytes")
    assert result["score"] is None
    assert result["provider"] in {"none", "error"}


def test_blocked_labels_parse():
    labels = mod._blocked_labels()
    assert "explicit" in labels and "nudity" in labels


@pytest.mark.parametrize(
    "score,expected",
    [(0.0, "approve"), (0.19, "approve"), (0.2, "manual"), (0.79, "manual"), (0.8, "reject")],
)
def test_threshold_boundaries(score, expected):
    assert mod.decide_action(score) == expected

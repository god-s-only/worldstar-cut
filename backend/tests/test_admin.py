import pytest
from fastapi.testclient import TestClient
from pydantic import ValidationError

from app.main import app
from app.schemas.admin import PayoutCreate, RejectPayload

client = TestClient(app)


def test_review_queue_requires_auth():
    response = client.get("/api/v1/admin/review-queue")
    assert response.status_code == 401


def test_payouts_require_auth():
    response = client.get("/api/v1/admin/payouts")
    assert response.status_code == 401


def test_reject_payload_requires_reason():
    with pytest.raises(ValidationError):
        RejectPayload(reason="")


def test_payout_rejects_zero_amount():
    with pytest.raises(ValidationError):
        PayoutCreate(
            seller_id="00000000-0000-0000-0000-000000000001", amount_cents=0
        )

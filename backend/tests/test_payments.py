import pytest
from fastapi.testclient import TestClient

from app.main import app
from app.services.stripe_service import compute_fee

client = TestClient(app)


def test_compute_fee_percent():
    assert compute_fee(1000) == 100  # 10%
    assert compute_fee(0) == 0
    assert compute_fee(999) == 100  # rounds to nearest cent


def test_webhook_rejects_bad_signature():
    response = client.post(
        "/webhooks/stripe",
        content=b'{"type": "checkout.session.completed"}',
        headers={"Stripe-Signature": "t=1,v1=deadbeef"},
    )
    assert response.status_code in {400, 503}


def test_webhook_requires_signature_header():
    response = client.post("/webhooks/stripe", content=b"{}")
    assert response.status_code in {400, 503}

from app.core.security import (
    create_access_token,
    decode_access_token,
    hash_password,
    verify_password,
)


def test_password_hash_roundtrip():
    hashed = hash_password("super-secret-123")
    assert hashed != "super-secret-123"
    assert verify_password("super-secret-123", hashed)
    assert not verify_password("wrong-password", hashed)


def test_password_hashes_are_salted():
    assert hash_password("same") != hash_password("same")


def test_jwt_roundtrip():
    token = create_access_token("11111111-2222-3333-4444-555555555555")
    payload = decode_access_token(token)
    assert payload is not None
    assert payload["sub"] == "11111111-2222-3333-4444-555555555555"
    assert "exp" in payload


def test_jwt_invalid_token_returns_none():
    assert decode_access_token("not-a-real-token") is None


def test_jwt_tampered_signature_returns_none():
    token = create_access_token("user-1")
    tampered = token[:-3] + ("aaa" if token[-3:] != "aaa" else "bbb")
    assert decode_access_token(tampered) is None

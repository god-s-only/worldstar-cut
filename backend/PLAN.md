# WorldstarCut Backend — Build Plan

> FastAPI + Postgres + Redis + OCI Object Storage + Stripe. Marketplace where any user can create account and sell sticker/animation packs. Mobile client is Android (Kotlin, Compose, Media3). Infra runs on Oracle Cloud (OCI).

## 0. Stack
- **API:** FastAPI 0.115, Pydantic 2, `pydantic-settings`
- **DB:** Postgres 16, SQLAlchemy 2 async, Alembic, `asyncpg`
- **Cache/Queue:** Redis + Celery (or ARQ) for moderation, thumbnail generation, payouts
- **Storage:** OCI Object Storage (S3-compatible API via `boto3`; MinIO for local dev) — buckets: `wsc-packs`, `wsc-previews`, `wsc-exports`
- **Auth:** JWT (`python-jose` + `passlib[bcrypt]`), OAuth2 password flow; Google OAuth via `httpx`
- **Payments:** Stripe + Stripe Connect Express (KYC for sellers), webhooks
- **Search:** Postgres `tsvector`/`pg_trgm` initially, Meilisearch later
- **Moderation:** OCI Vision (NSFW) + manual review queue
- **Deploy:** Docker on OCI Compute (docker-compose: api, db, redis, worker), Alembic migrations; OKE later if needed

## 1. Project Structure (target)
```
backend/
  app/
    main.py                 # FastAPI + CORS + routers
    core/
      config.py             # Settings (env)
      security.py           # JWT create/verify, password hash
      deps.py               # get_current_user, get_db
    db/
      session.py            # async engine + get_db
      base.py               # declarative_base + import models
    models/
      user.py               # User, Profile
      pack.py               # Pack, PackItem, Tag
      marketplace.py        # Listing, Purchase, Entitlement, Payout
      moderation.py         # ReviewQueue
    schemas/
      user.py               # UserCreate, UserOut, Token
      pack.py               # PackCreate/Update/Out, PackItem
      marketplace.py        # ListingCreate/Out, PurchaseOut
    api/
      v1/
        health.py           # /health, /ready
        auth.py             # POST /auth/register, /login, /google, /me
        packs.py            # CRUD packs, upload items
        marketplace.py      # listings, search, purchase
        webhooks/
          stripe.py         # POST /webhooks/stripe
        admin/
          moderation.py     # review queue
    services/
      storage.py            # S3 upload, presigned URLs
      pack_service.py       # pack validation, thumbnail
      marketplace_service.py# listing, purchase, entitlement
      moderation_service.py # Vision check + queue
      stripe_service.py     # Connect onboarding, checkout, webhook
    workers/
      tasks.py              # Celery: thumbnail, moderation, payout
  alembic/
  tests/
  requirements.txt
  Dockerfile
  docker-compose.yml
  .env.example
```

## 2. Data Model (Postgres)

### users (Supabase-style, but FastAPI-owned)
- `id UUID PK`, `email UNIQUE`, `password_hash`, `display_name`, `avatar_url`, `is_creator BOOL`, `stripe_account_id TEXT NULL`, `kyc_status TEXT`, `created_at`

### packs
- `id UUID PK`, `owner_id FK users`, `title`, `description`, `cover_url`, `is_public BOOL`, `created_at`, `updated_at`
- `pack_items`: `id UUID PK`, `pack_id FK`, `type TEXT` (sticker=lottie/png/webp, animation=json), `file_url`, `preview_url`, `width`, `height`, `duration_ms`, `order`

### marketplace_listings
- `id UUID PK`, `pack_id FK packs UNIQUE`, `seller_id FK users`, `price_cents INT`, `currency TEXT`, `status TEXT` (pending/approved/rejected), `reject_reason TEXT`, `downloads INT`, `rating_avg`, `created_at`
- Full-text index on `packs.title + description + tags`

### purchases / entitlements / payouts
- `purchases`: `id UUID PK`, `buyer_id FK`, `listing_id FK`, `stripe_checkout_id`, `amount_cents`, `fee_cents`, `status`
- `entitlements`: `buyer_id`, `pack_id` PK composite — grants download
- `payouts`: `id UUID PK`, `seller_id FK`, `amount_cents`, `stripe_transfer_id`, `status`

### moderation
- `review_queue`: `id UUID PK`, `listing_id FK`, `vision_score JSONB`, `status`, `reviewer_id`, `created_at`

## 3. Auth
- `POST /api/v1/auth/register` {email, password, display_name} → `access_token` (JWT, 7d)
- `POST /api/v1/auth/login` → token
- `POST /api/v1/auth/google` {id_token} → verify via Google, upsert user
- `GET /api/v1/auth/me` (Bearer) → UserOut
- `POST /api/v1/auth/connect/onboard` (creator) → Stripe Connect Express onboarding link
- Passwords `bcrypt`, JWT `HS256` via `SECRET_KEY`.

## 4. Packs (UGC)
- `POST /api/v1/packs` (auth) {title, description, tags, is_public} → pack
- `POST /api/v1/packs/{pack_id}/items` (multipart) → upload to `wsc-packs/{pack_id}/{uuid}.ext`, generate preview, store `pack_items`
- `GET /api/v1/packs/me` → my packs
- `GET /api/v1/packs/{pack_id}` → pack + items (presigned URLs)
- `PUT /api/v1/packs/{pack_id}` `DELETE ...`
- Pack validation: `pack.json` schema (max 30 items, each <2MB, allowed types). Stored as zip `.wspack` optionally.

## 5. Marketplace
- `POST /api/v1/marketplace/listings` (auth, is_creator) {pack_id, price_cents} → creates `pending` listing, enqueues moderation
- `GET /api/v1/marketplace/listings?q=&tag=&sort=&page=` → approved only, `tsvector` search, pagination 20
- `GET /api/v1/marketplace/listings/{id}` → detail + preview URLs (signed, 1h)
- `POST /api/v1/marketplace/listings/{id}/purchase` (auth) → Stripe Checkout Session (with `application_fee_amount`), returns `checkout_url`
- `POST /api/v1/webhooks/stripe` → on `checkout.session.completed` → create `purchase` + `entitlement`, increment `downloads`
- `GET /api/v1/marketplace/entitlements/me` → packs I own (for mobile to download)
- `GET /api/v1/marketplace/packs/{pack_id}/download` (auth, has entitlement or free) → 302 to presigned S3 URL or zip

## 6. Moderation
- On listing create: worker runs Vision (NSFW, copyright via image hash), writes `review_queue`, auto-reject if score > threshold else `pending` for manual.
- `GET /api/v1/admin/review-queue` (admin role) + `POST /{id}/approve` / `reject` → updates `listings.status`, moves files `pending_packs/` → `public_packs/`.

## 7. Storage (OCI Object Storage — S3-compatible)
- `boto3` client with `endpoint_url=https://<namespace>.compat.objectstorage.<region>.oraclecloud.com`, auth via OCI Customer Secret Keys
- `s3.upload_fileobj` with content-type sniff, `CacheControl: max-age=31536000`
- Previews: 512x512 webp generated via Pillow worker
- Presigned GET 3600s for downloads; PUT via presigned POST for uploads (or direct multipart via API).

## 8. Search & Discovery
- `packs` `tsvector` on `title || description || tags`
- `GET /marketplace/listings?q=glitch` → `plainto_tsquery` + `pg_trgm` fallback
- Later: Meilisearch sync via worker on listing approve.

## 9. Mobile Integration
- Android: `Retrofit` + `AuthInterceptor` (Bearer), `DataStore` for token
- Flow: Create pack in editor → `POST /packs` + upload items → `POST /marketplace/listings` → wait `approved` → appears in `GET /listings`
- Purchase: `POST /listings/{id}/purchase` → open `checkout_url` in Custom Tab → Stripe webhook grants entitlement → `GET /entitlements/me` → download zip → unzip to `files/marketplace_packs/{pack_id}/`

## 10. Build Order (you can parallelize)
1. **Auth + DB** — Alembic init, `users` + JWT, `GET /me`
2. **Packs CRUD** — `packs` + `pack_items` + S3 upload
3. **Marketplace read** — listings search, `GET /listings`
4. **Moderation** — Vision worker + review queue
5. **Stripe Connect + purchase** — onboarding, checkout, webhook, entitlements
6. **Admin + payouts** — Connect transfers, cron

## 11. Env & Run
- `cp .env.example .env` → set `DATABASE_URL`, `REDIS_URL`, `OBJECT_STORAGE_*`, `STRIPE_*`, `SECRET_KEY`
- `alembic upgrade head` → `uvicorn app.main:app --reload`
- `celery -A app.workers.celery worker -l info` + `beat`
- `docker-compose up --build` for full stack

---
*This plan is the source of truth — implement in order; mobile can mock with local packs until `GET /listings` is live.*

# WorldstarCut Backend — FastAPI

## Quickstart
```bash
python -m venv .venv && source .venv/bin/activate  # Windows: .venv\Scripts\activate
pip install -r requirements.txt
cp .env.example .env  # edit
uvicorn app.main:app --reload --port 8000
```
Docs: http://localhost:8000/docs — Health: http://localhost:8000/api/v1/health

## Structure
```
app/
  main.py          # FastAPI app + health
  core/config.py   # settings
  api/v1/health.py # health router
  db/session.py    # async SQLAlchemy
  models/          # SQLAlchemy models (TODO)
  schemas/         # Pydantic schemas (TODO)
  services/        # business logic (TODO)
```

## Env
See `.env.example`. Postgres + Redis required for full stack (docker-compose coming).

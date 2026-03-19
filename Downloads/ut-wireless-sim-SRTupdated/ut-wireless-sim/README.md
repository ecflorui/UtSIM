# UT Austin Wireless Coverage Simulator

Full-stack interactive 802.11 coverage map with Sionna RT ray tracing backend.

## Architecture

```
┌─────────────────────┐        POST /coverage        ┌──────────────────────┐
│   Next.js Frontend  │ ──────────────────────────▶  │  FastAPI + Sionna RT │
│   (Vercel)          │ ◀──────────────────────────  │  (GPU server)        │
│                     │    {cells, coverage%, engine} │                      │
│  Leaflet map        │                               │  RadioMapSolver      │
│  Canvas heatmap     │        GET /buildings         │  OSM scene           │
│  Sidebar controls   │ ──────────────────────────▶  │  Mitsuba 3 geometry  │
└─────────────────────┘                               └──────────────────────┘
```

## Quick Start

### 1. Backend (Python)
```bash
cd backend
python -m venv venv && source venv/bin/activate   # Windows: venv\Scripts\activate
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
```

### 2. Frontend (Next.js)
```bash
cp .env.local.example .env.local   # set NEXT_PUBLIC_BACKEND_URL
npm install
npm run dev
```

Open http://localhost:3000

## Deploy
- **Frontend** → Vercel (push to GitHub, import repo)
- **Backend** → Any GPU server (RunPod, Modal, AWS g4dn) running uvicorn

See `backend/README.md` for full backend deployment guide.

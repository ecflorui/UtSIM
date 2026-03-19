# UT Wireless Sim — Python Backend

## Stack
- **FastAPI** — REST API
- **Sionna RT 1.2.1** — physics-accurate ray tracing radio maps
- **Mitsuba 3** — 3D scene renderer (Sionna dependency)
- **Overpass API** — fetches real UT Austin building footprints from OpenStreetMap

## Setup

### 1. Python environment
```bash
cd backend
python -m venv venv

# Windows
venv\Scripts\activate

# Mac/Linux
source venv/bin/activate

pip install -r requirements.txt
```

### 2. Sionna RT requires LLVM (for CPU) or CUDA (for GPU)

**CPU only (dev machine / no GPU):**
```bash
# Install LLVM backend for Dr.Jit
# Windows: download LLVM from https://releases.llvm.org/
# Mac:     brew install llvm
# Linux:   apt install llvm
```

**GPU (recommended for production):**
- Requires NVIDIA GPU with CUDA 11.8+
- Sionna will auto-detect and use it

> **Note:** If Sionna fails to load (no GPU/LLVM), the backend automatically
> falls back to a fast log-distance path loss model so the app still works.

### 3. Run the backend
```bash
uvicorn main:app --reload --port 8000
```

On first run it will:
1. Fetch UT Austin buildings from OpenStreetMap (~2 seconds)
2. Build a Mitsuba 3 scene XML + OBJ mesh
3. Load the Sionna RT scene (GPU: ~5s, CPU: ~15s)

Cached in `scene_cache/` — subsequent starts are instant.

### 4. Run the frontend
```bash
cd ..   # back to project root
npm run dev
```

Frontend at http://localhost:3000, backend at http://localhost:8000.

## API

### `POST /coverage`
```json
{
  "stations": [{"id": "1", "lat": 30.2849, "lng": -97.7341}],
  "tx_power_dbm": 20,
  "frequency_ghz": 2.4,
  "noise_floor_dbm": -101,
  "snr_threshold_db": 10
}
```
Returns:
```json
{
  "cells": [{"lat": 30.281, "lng": -97.733, "rssi": -65.2, "station_idx": 0}],
  "coverage_pct": 34.5,
  "max_range_m": 312.0,
  "engine": "sionna"
}
```

### `GET /buildings`
Returns GeoJSON FeatureCollection of campus building footprints.

### `GET /health`
Returns `{"status": "ok"}`.

## Deploy backend to a GPU server

Options:
- **RunPod / Vast.ai** — rent a GPU instance (~$0.20/hr for RTX 3090)
- **AWS EC2 g4dn.xlarge** — NVIDIA T4, good for Sionna
- **Modal.com** — serverless GPU, pay per invocation

Set `BACKEND_URL` in your Vercel environment variables to point the frontend to your backend.

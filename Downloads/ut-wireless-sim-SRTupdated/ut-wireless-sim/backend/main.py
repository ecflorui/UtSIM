"""
main.py  –  UT Wireless Coverage Simulator backend
"""
import logging, math
from contextlib import asynccontextmanager
from typing import List

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

from scene_builder import build_scene, fetch_buildings, UT_BBOX, LAT0
from sionna_solver import init_scene, compute_coverage, RadioParams

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Cached buildings for fallback LOS (loaded once at startup)
_buildings_cache: List = []

@asynccontextmanager
async def lifespan(app: FastAPI):
    global _buildings_cache
    logger.info("Starting up…")
    try:
        # Fetch + cache building polygons
        data = fetch_buildings()
        _buildings_cache = [
            el["geometry"]
            for el in data.get("elements", [])
            if el.get("type") == "way" and len(el.get("geometry", [])) >= 3
        ]
        logger.info(f"Loaded {len(_buildings_cache)} building polygons")

        # Build Mitsuba scene + init Sionna
        scene_path = build_scene()
        init_scene(scene_path)
    except Exception as e:
        logger.error(f"Startup error: {e}")
    yield

app = FastAPI(title="UT Wireless Simulator API", lifespan=lifespan)
app.add_middleware(CORSMiddleware, allow_origins=["*"],
                   allow_methods=["*"], allow_headers=["*"])

# ── Models ────────────────────────────────────────────────────────────────────
class Station(BaseModel):
    id: str
    lat: float
    lng: float

class CoverageRequest(BaseModel):
    stations: List[Station]
    tx_power_dbm:    float = Field(default=20.0,   ge=-30, le=40)
    frequency_ghz:   float = Field(default=2.4,    ge=0.4, le=7.0)
    noise_floor_dbm: float = Field(default=-101.0, ge=-120, le=-60)
    snr_threshold_db: float = Field(default=10.0,  ge=0,   le=40)

class CoverageCell(BaseModel):
    lat: float; lng: float; rssi: float; station_idx: int

class CoverageResponse(BaseModel):
    cells: list  # Bypass Pydantic validation for 48,000 elements for speed
    coverage_pct: float
    max_range_m: float
    engine: str

class BuildingsResponse(BaseModel):
    type: str = "FeatureCollection"
    features: list

# ── Endpoints ─────────────────────────────────────────────────────────────────
@app.get("/health")
def health():
    from sionna_solver import _sionna_ok
    return {"status": "ok", "engine": "sionna" if _sionna_ok else "fallback",
            "buildings_loaded": len(_buildings_cache)}

from fastapi.responses import JSONResponse

@app.post("/coverage")
def coverage(req: CoverageRequest):
    if not req.stations:
        return JSONResponse(content={"cells": [], "coverage_pct": 0.0, "max_range_m": 0.0, "engine": "none"})
    from sionna_solver import _sionna_ok
    params = RadioParams(
        tx_power_dbm=req.tx_power_dbm,
        frequency_ghz=req.frequency_ghz,
        noise_floor_dbm=req.noise_floor_dbm,
        snr_threshold_db=req.snr_threshold_db,
    )
    stations_dicts = [{"id": s.id, "lat": s.lat, "lng": s.lng} for s in req.stations]
    try:
        cells = compute_coverage(stations_dicts, params, _buildings_cache)
    except Exception as e:
        logger.error(f"Coverage error: {e}")
        raise HTTPException(status_code=500, detail=str(e))

    # Coverage %
    lat_min, lon_min, lat_max, lon_max = UT_BBOX
    R = 6_371_000.0
    lat_m = (lat_max-lat_min)*math.pi/180*R
    lng_m = (lon_max-lon_min)*math.pi/180*R*math.cos(math.radians((lat_min+lat_max)/2))
    total = (lat_m/6)*(lng_m/6)
    pct   = min(100.0, len(cells)/max(total,1)*100)

    # Max range
    max_range = 0.0
    for c in cells:
        for s in req.stations:
            dlat = math.radians(c["lat"]-s.lat)
            dlng = math.radians(c["lng"]-s.lng)
            a = math.sin(dlat/2)**2 + \
                math.cos(math.radians(s.lat))*math.cos(math.radians(c["lat"]))*math.sin(dlng/2)**2
            d = R*2*math.atan2(math.sqrt(a), math.sqrt(1-a))
            if d > max_range: max_range = d

    engine = "sionna" if _sionna_ok else "fallback"
    return JSONResponse(content={
        "cells": cells,
        "coverage_pct": round(pct, 2),
        "max_range_m": round(max_range, 1),
        "engine": engine,
    })

@app.get("/buildings", response_model=BuildingsResponse)
def buildings():
    try:
        data = fetch_buildings()
    except Exception as e:
        raise HTTPException(status_code=503, detail=str(e))
    features = []
    for el in data.get("elements", []):
        if el.get("type") != "way": continue
        geom = el.get("geometry", [])
        if len(geom) < 3: continue
        coords = [[n["lon"], n["lat"]] for n in geom]
        if coords[0] != coords[-1]: coords.append(coords[0])
        features.append({
            "type": "Feature",
            "geometry": {"type": "Polygon", "coordinates": [coords]},
            "properties": {"id": el.get("id"), **el.get("tags", {})},
        })
    return {"type": "FeatureCollection", "features": features}

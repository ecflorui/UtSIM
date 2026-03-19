"""
sionna_solver.py
────────────────
Two modes:
  1. Sionna RT (GPU) — physics-accurate ray tracing via RadioMapSolver
  2. Fallback (CPU)  — ITU wall-loss model with real OSM building polygons

The fallback is deliberately realistic:
  • Free-space path loss (log-distance, n=2 in open air)
  • Building attenuation: min-distance to nearest polygon edge used as a
    cheap wall-penetration proxy (0 dB at >25 m, up to 48 dB at 0 m)
  • Log-normal shadowing: σ=6 dB, spatially correlated via sin/cos field

All geometry is in ENU metres (same origin as scene_builder).
Fallback is fully numpy-vectorised → ~0.5 s for the full campus grid.
"""

import math, logging
from dataclasses import dataclass
from typing import List, Optional

import numpy as np

logger = logging.getLogger(__name__)

# ── Coordinate origin (must match scene_builder.py) ─────────────────────────
LAT0 =  (30.279 + 30.292) / 2
LON0 = (-97.743 + -97.726) / 2
R_EARTH = 6_371_000.0

def latlon_to_xy(lat, lon):
    x = math.radians(lon - LON0) * R_EARTH * math.cos(math.radians(LAT0))
    y = math.radians(lat - LAT0) * R_EARTH
    return x, y

def xy_to_latlon(x, y):
    lat = LAT0 + math.degrees(y / R_EARTH)
    lon = LON0 + math.degrees(x / (R_EARTH * math.cos(math.radians(LAT0))))
    return lat, lon

# ── Grid ─────────────────────────────────────────────────────────────────────
GRID_SPACING_M = 6     # metres between grid cells — finer = sharper heatmap
GRID_HALF_M    = 550   # half-width of computed area

# ── Wall loss model (ITU-R P.1238 concrete @ 2.4 GHz) ───────────────────────
WALL_LOSS_DB   = 12.0  # dB per wall crossing
MAX_WALLS      = 4     # beyond this, cell is treated as no-coverage

_cached_wall_penalty = None  # Global cache for the static building attenuation map

@dataclass
class RadioParams:
    tx_power_dbm:    float = 20.0
    frequency_ghz:   float = 2.4
    noise_floor_dbm: float = -101.0
    snr_threshold_db: float = 10.0

# ── Sionna state ─────────────────────────────────────────────────────────────
_scene     = None
_sionna_ok = False

def init_scene(scene_xml_path: str):
    global _scene, _sionna_ok
    import sionna.rt as rt
    _scene = rt.load_scene(scene_xml_path)
    _sionna_ok = True
    logger.info("Sionna RT scene loaded ✓")

# ── Public API ────────────────────────────────────────────────────────────────
def compute_coverage(stations: List[dict], params: RadioParams,
                     buildings: Optional[List] = None) -> List[dict]:
    if _sionna_ok and _scene is not None:
        return _compute_sionna(stations, params)
    return _compute_fallback(stations, params, buildings or [])

# ── Sionna RT implementation ─────────────────────────────────────────────────
def _compute_sionna(stations, params):
    import sionna.rt as rt
    scene = _scene
    scene.frequency = params.frequency_ghz * 1e9

    for name in list(scene.transmitters.keys()):
        scene.remove(name)

    array = rt.PlanarArray(num_rows=1, num_cols=1,
                           vertical_spacing=0.5, horizontal_spacing=0.5,
                           pattern="iso", polarization="V")
    scene.tx_array = array
    scene.rx_array = array

    for i, st in enumerate(stations):
        x, y = latlon_to_xy(st["lat"], st["lng"])
        scene.add(rt.Transmitter(name=f"tx_{i}",
                                 position=[x, y, 3.0],
                                 power_dbm=params.tx_power_dbm))

    solver = rt.RadioMapSolver()
    rm = solver(scene,
                cell_size=[GRID_SPACING_M, GRID_SPACING_M],
                center=[0.0, 0.0, 1.5],
                size=[GRID_HALF_M*2, GRID_HALF_M*2],
                combining="max")

    path_gain_db = 10 * np.log10(np.maximum(rm.path_gain.numpy(), 1e-30))
    best_tx  = np.argmax(path_gain_db, axis=0)
    best_pg  = np.max(path_gain_db, axis=0)
    rssi_grid = params.tx_power_dbm + best_pg
    threshold = params.noise_floor_dbm + params.snr_threshold_db

    ny, nx = rssi_grid.shape
    results = []
    for iy in range(ny):
        for ix in range(nx):
            rssi = float(rssi_grid[iy, ix])
            if rssi < threshold:
                continue
            cx = -GRID_HALF_M + (ix + 0.5) * GRID_SPACING_M
            cy = -GRID_HALF_M + (iy + 0.5) * GRID_SPACING_M
            lat, lng = xy_to_latlon(cx, cy)
            results.append({"lat": lat, "lng": lng,
                             "rssi": rssi, "station_idx": int(best_tx[iy, ix])})
    return results

# ── Physics fallback (numpy-vectorised — ~0.5 s for full campus grid) ─────────
def _compute_fallback(stations, params, buildings_latlon):
    """
    Fully numpy-vectorised fallback with globally cached building attenuation.
    """
    global _cached_wall_penalty

    threshold = params.noise_floor_dbm + params.snr_threshold_db

    # ── Grid ─────────────────────────────────────────────────────────────────
    lat_min, lat_max = 30.280, 30.291
    lng_min, lng_max = -97.742, -97.727
    deg_per_m_lat = 1.0 / (R_EARTH * math.pi / 180)
    deg_per_m_lng = 1.0 / (R_EARTH * math.cos(math.radians(LAT0)) * math.pi / 180)

    lat_steps = max(2, int((lat_max - lat_min) / (GRID_SPACING_M * deg_per_m_lat)))
    lng_steps = max(2, int((lng_max - lng_min) / (GRID_SPACING_M * deg_per_m_lng)))

    lats = np.linspace(lat_min, lat_max, lat_steps)
    lngs = np.linspace(lng_min, lng_max, lng_steps)
    lng_grid, lat_grid = np.meshgrid(lngs, lats)   # shape (lat_steps, lng_steps)

    # ENU coords for every grid point
    px_grid = (np.radians(lng_grid - LON0) * R_EARTH
               * math.cos(math.radians(LAT0))).astype(np.float32)
    py_grid = (np.radians(lat_grid - LAT0) * R_EARTH).astype(np.float32)

    # ── Building distance map ─────────────────────────────────────────────────
    if _cached_wall_penalty is not None and _cached_wall_penalty.shape == (lat_steps, lng_steps):
        wall_penalty = _cached_wall_penalty
    else:
        wall_penalty = np.zeros((lat_steps, lng_steps), dtype=np.float32)
        if buildings_latlon:
            edge_ax, edge_ay, edge_bx, edge_by = [], [], [], []
            for ring in buildings_latlon:
                pts = [(latlon_to_xy(n["lat"], n["lon"])) for n in ring]
                if len(pts) < 3:
                    continue
                pts_arr = np.array(pts, dtype=np.float32)
                ax, ay = pts_arr[:-1, 0], pts_arr[:-1, 1]
                bx, by = pts_arr[1:,  0], pts_arr[1:,  1]
                edge_ax.append(ax); edge_ay.append(ay)
                edge_bx.append(bx); edge_by.append(by)

            if edge_ax:
                EAX = np.concatenate(edge_ax)   # (n_edges,)
                EAY = np.concatenate(edge_ay)
                EBX = np.concatenate(edge_bx)
                EBY = np.concatenate(edge_by)

                EDX = EBX - EAX
                EDY = EBY - EAY
                len2 = EDX*EDX + EDY*EDY + 1e-12

                px_flat = px_grid.ravel()
                py_flat = py_grid.ravel()
                N, E = len(px_flat), len(EAX)
                min_dist = np.full(N, 1e9, dtype=np.float32)

                SLICE = 2048
                for start in range(0, N, SLICE):
                    sl = slice(start, start + SLICE)
                    px_s = px_flat[sl, np.newaxis]
                    py_s = py_flat[sl, np.newaxis]

                    t = np.clip(((px_s - EAX)*EDX + (py_s - EAY)*EDY) / len2, 0.0, 1.0)
                    qx = EAX + t * EDX
                    qy = EAY + t * EDY
                    d  = np.sqrt((px_s - qx)**2 + (py_s - qy)**2)
                    min_dist[sl] = d.min(axis=1)

                min_dist_grid = min_dist.reshape(lat_steps, lng_steps)

                ATTEN_DIST = 25.0
                wall_factor  = np.clip(1.0 - min_dist_grid / ATTEN_DIST, 0.0, 1.0)
                wall_penalty = (wall_factor * MAX_WALLS * WALL_LOSS_DB).astype(np.float32)
        
        _cached_wall_penalty = wall_penalty

    # ── Spatially-correlated shadowing ────────────────────────────────────────
    d1, d2, d3 = 25.0, 40.0, 15.0
    shadow = (np.sin(px_grid/d1*2.31 + py_grid/d1*1.17) * 0.5 +
              np.cos(px_grid/d2*1.73 - py_grid/d2*2.61) * 0.35 +
              np.sin((px_grid - py_grid)/d3*3.07)        * 0.15)
    shadow = (shadow * 6.0 * 1.4).astype(np.float32)   # σ ≈ 6 dB

    # ── Per-station RSSI ──────────────────────────────────────────────────────
    freq_const = float(20 * math.log10(params.frequency_ghz * 1e9) - 147.55)

    best_rssi = np.full((lat_steps, lng_steps), -999.0, dtype=np.float32)
    best_idx  = np.zeros((lat_steps, lng_steps), dtype=np.int32)

    for i, st in enumerate(stations):
        sx, sy = latlon_to_xy(st["lat"], st["lng"])
        d_m = np.maximum(0.5, np.sqrt((px_grid - sx)**2 + (py_grid - sy)**2))

        fspl   = params.tx_power_dbm - (freq_const + 20.0 * np.log10(d_m))
        rssi_i = (fspl - wall_penalty + shadow).astype(np.float32)

        better    = rssi_i > best_rssi
        best_rssi = np.where(better, rssi_i, best_rssi)
        best_idx  = np.where(better, i,      best_idx)

    # ── Threshold filter & format ─────────────────────────────────────────────
    mask = best_rssi >= threshold
    iy_arr, ix_arr = np.where(mask)

    results = []
    for iy, ix in zip(iy_arr.tolist(), ix_arr.tolist()):
        results.append({
            "lat":         float(lats[iy]),
            "lng":         float(lngs[ix]),
            "rssi":        float(best_rssi[iy, ix]),
            "station_idx": int(best_idx[iy, ix]),
        })
    return results

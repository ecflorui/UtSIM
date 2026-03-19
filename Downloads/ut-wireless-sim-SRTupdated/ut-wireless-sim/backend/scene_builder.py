"""
scene_builder.py
────────────────
Downloads UT Austin building footprints from OpenStreetMap via Overpass API
and writes a Mitsuba 3 / Sionna RT scene XML.

Buildings are extruded to a default height of 10 m (flat-roof boxes).
The ground plane is a large concrete slab.
"""

import os, json, math, textwrap, hashlib, requests
from pathlib import Path

# ── UT Austin campus bounding box ───────────────────────────────────────────
UT_BBOX = (30.279, -97.743, 30.292, -97.726)   # (lat_min, lon_min, lat_max, lon_max)
SCENE_CACHE = Path(__file__).parent / "scene_cache"
SCENE_XML    = SCENE_CACHE / "ut_campus.xml"
DEFAULT_HEIGHT_M = 10.0  # extrusion height for buildings without height tag

# ── WGS-84 → local ENU (east-north-up) meters ───────────────────────────────
LAT0 = (UT_BBOX[0] + UT_BBOX[2]) / 2
LON0 = (UT_BBOX[1] + UT_BBOX[3]) / 2
R_EARTH = 6_371_000.0

def latlon_to_xy(lat, lon):
    x = math.radians(lon - LON0) * R_EARTH * math.cos(math.radians(LAT0))
    y = math.radians(lat - LAT0) * R_EARTH
    return x, y

def xy_to_latlon(x, y):
    lat = LAT0 + math.degrees(y / R_EARTH)
    lon = LON0 + math.degrees(x / (R_EARTH * math.cos(math.radians(LAT0))))
    return lat, lon

# ── Overpass query ───────────────────────────────────────────────────────────
def fetch_buildings(force=False):
    cache_json = SCENE_CACHE / "buildings.json"
    SCENE_CACHE.mkdir(exist_ok=True)
    if cache_json.exists() and not force:
        with open(cache_json) as f:
            return json.load(f)

    lat_min, lon_min, lat_max, lon_max = UT_BBOX
    query = f"""
    [out:json][timeout:30];
    (
      way["building"]({lat_min},{lon_min},{lat_max},{lon_max});
      relation["building"]({lat_min},{lon_min},{lat_max},{lon_max});
    );
    out geom;
    """
    resp = requests.post(
        "https://overpass-api.de/api/interpreter",
        data={"data": query},
        timeout=60,
    )
    resp.raise_for_status()
    data = resp.json()
    with open(cache_json, "w") as f:
        json.dump(data, f)
    return data

# ── Build extruded box mesh from a polygon footprint ────────────────────────
def polygon_to_obj_faces(ring_xy, height, vert_offset):
    """
    Extrudes a 2-D polygon ring upward by `height`.
    Returns (vertices, faces) in OBJ format indices (1-based, offset by vert_offset).
    """
    n = len(ring_xy)
    verts = []
    # Bottom ring (z=0), top ring (z=height)
    for x, y in ring_xy:
        verts.append((x, y, 0.0))
    for x, y in ring_xy:
        verts.append((x, y, height))

    faces = []
    # Side walls
    for i in range(n):
        j = (i + 1) % n
        b0, b1 = vert_offset + i + 1, vert_offset + j + 1
        t0, t1 = vert_offset + n + i + 1, vert_offset + n + j + 1
        faces.append((b0, b1, t1, t0))
    # Top cap (fan triangulation)
    top_base = vert_offset + n + 1
    for i in range(1, n - 1):
        faces.append((top_base, top_base + i, top_base + i + 1))

    return verts, faces

# ── Write Mitsuba-compatible OBJ + MTL + XML ────────────────────────────────
def build_scene(force=False):
    if SCENE_XML.exists() and not force:
        return str(SCENE_XML)

    SCENE_CACHE.mkdir(exist_ok=True)
    data = fetch_buildings(force)

    all_verts = []
    all_faces = []   # list of (face_tuple, material)
    vert_offset = 0

    ground_half = 600  # meters
    gv = [(-ground_half, -ground_half, 0), ( ground_half, -ground_half, 0),
          ( ground_half,  ground_half, 0), (-ground_half,  ground_half, 0)]
    all_verts.extend(gv)
    all_faces.append(((1, 2, 3, 4), "itu_concrete"))
    vert_offset += 4

    for el in data.get("elements", []):
        if el.get("type") != "way":
            continue
        geom = el.get("geometry", [])
        if len(geom) < 3:
            continue

        ring_xy = [latlon_to_xy(node["lat"], node["lon"]) for node in geom]
        # Close ring if needed
        if ring_xy[0] != ring_xy[-1]:
            ring_xy.append(ring_xy[0])
        ring_xy = ring_xy[:-1]  # remove duplicate closing point

        tags = el.get("tags", {})
        try:
            h = float(tags.get("height", tags.get("building:levels", 3)) )
            if "building:levels" in tags and "height" not in tags:
                h = h * 3.5
        except (ValueError, TypeError):
            h = DEFAULT_HEIGHT_M

        verts, faces = polygon_to_obj_faces(ring_xy, h, vert_offset)
        all_verts.extend(verts)
        for f in faces:
            all_faces.append((f, "itu_concrete"))
        vert_offset += len(verts)

    # Write OBJ
    obj_path = SCENE_CACHE / "ut_campus.obj"
    with open(obj_path, "w") as f:
        f.write("mtllib ut_campus.mtl\n")
        for v in all_verts:
            f.write(f"v {v[0]:.3f} {v[1]:.3f} {v[2]:.3f}\n")
        cur_mat = None
        for face, mat in all_faces:
            if mat != cur_mat:
                f.write(f"usemtl {mat}\n")
                cur_mat = mat
            f.write("f " + " ".join(str(i) for i in face) + "\n")

    # Write MTL
    mtl_path = SCENE_CACHE / "ut_campus.mtl"
    with open(mtl_path, "w") as f:
        f.write("newmtl itu_concrete\nKd 0.4 0.4 0.4\n\n")

    # Write Mitsuba XML scene
    xml = textwrap.dedent(f"""\
    <?xml version="1.0" encoding="utf-8"?>
    <scene version="3.0.0">
        <integrator type="path"/>

        <!-- Ground + Buildings -->
        <shape type="obj" id="campus_mesh">
            <string name="filename" value="ut_campus.obj"/>
        </shape>

        <!-- Radio materials (Sionna RT reads these by material name) -->
        <bsdf type="sionna_material" id="itu_concrete">
            <string name="material_name" value="itu_concrete"/>
        </bsdf>
    </scene>
    """)
    with open(SCENE_XML, "w") as f:
        f.write(xml)

    return str(SCENE_XML)


if __name__ == "__main__":
    p = build_scene(force=True)
    print(f"Scene written to: {p}")

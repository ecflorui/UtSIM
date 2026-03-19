import requests, time

body = {
    "stations": [{"id": "1", "lat": 30.2849, "lng": -97.7341}],
    "tx_power_dbm": 20,
    "frequency_ghz": 2.4,
    "noise_floor_dbm": -101,
    "snr_threshold_db": 10
}
t0 = time.time()
r = requests.post("http://127.0.0.1:8000/coverage", json=body)
elapsed = time.time() - t0
d = r.json()
print(f"Time:     {elapsed:.2f}s")
print(f"Engine:   {d['engine']}")
print(f"Cells:    {len(d['cells'])}")
print(f"Coverage: {d['coverage_pct']}%")
if d["cells"]:
    c = d["cells"][0]
    print(f"Sample:   lat={c['lat']:.5f} lng={c['lng']:.5f} rssi={c['rssi']:.1f} dBm")

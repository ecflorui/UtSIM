from fastapi.testclient import TestClient
from main import app
import time

client = TestClient(app)
body = {
    "stations": [{"id": "1", "lat": 30.2849, "lng": -97.7341}],
    "tx_power_dbm": 20,
    "frequency_ghz": 2.4,
    "noise_floor_dbm": -101,
    "snr_threshold_db": 10
}

t0 = time.time()
print("Sending request...")
response = client.post("/coverage", json=body)
print(f"Elapsed: {time.time()-t0:.2f}s")
print("Status:", response.status_code)
if response.status_code != 200:
    print("Error:", response.text)
else:
    d = response.json()
    print("Engine:", d["engine"])
    print("Cells:", len(d["cells"]))

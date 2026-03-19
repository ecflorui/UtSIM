import time, cProfile, pstats
from main import coverage, CoverageRequest, Station

req = CoverageRequest(
    stations=[Station(id="1", lat=30.2849, lng=-97.7341)],
    tx_power_dbm=20,
    frequency_ghz=2.4,
    noise_floor_dbm=-101,
    snr_threshold_db=10
)

print("Starting coverage call...")
t0 = time.time()

# We will run this with cProfile
def run():
    return coverage(req)

profiler = cProfile.Profile()
profiler.enable()
res = run()
profiler.disable()

print(f"Total time: {time.time() - t0:.2f}s")
print(f"Cells returned: {len(res.cells)}")
print(f"Max range: {res.max_range_m}")
stats = pstats.Stats(profiler).sort_stats('tottime')
stats.print_stats(15)

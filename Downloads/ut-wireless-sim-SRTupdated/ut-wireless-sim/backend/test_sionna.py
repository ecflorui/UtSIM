import traceback
import sys

try:
    import sionna.rt as rt
    rt.load_scene('scene_cache/ut_campus.xml')
except Exception as e:
    with open('sionna_error.txt', 'w') as f:
        f.write(str(e))

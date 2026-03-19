import traceback
from scene_builder import build_scene
from sionna_solver import init_scene

try:
    scene_path = build_scene()
    init_scene(scene_path)
except Exception as e:
    with open('sionna_error.txt', 'w') as f:
        f.write(traceback.format_exc())

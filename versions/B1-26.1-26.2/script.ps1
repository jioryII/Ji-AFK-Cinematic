import os

with open('inspect_level_renderer.py', 'w') as f:
    f.write('''
import sys
import os

# We will just reuse the logic from inspect_camera.py
# that finds the jar and uses zipfile + javap.
# Actually we can just run javap on net.minecraft.client.renderer.LevelRenderer
# But we don't have javap. Wait! inspect_camera.py parses the class file or uses python bytecode tools?
# Let's see how inspect_camera.py works.
''')

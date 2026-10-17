"""Enhance the shankh model to match the sacred reference photo:
- Luminous ivory-white porcelain shell exterior (#FCFBF8)
- Warm natural peach/apricot inner lip (#F8E0D0)
- Smooth glossy shell finish (roughness ~0.24)
"""
import bpy

bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.gltf(filepath='android/app/src/main/assets/shrine/shankh/classic.glb')

# 1. Update Base Color texture
if 'Image_0' in bpy.data.images:
    tex = bpy.data.images['Image_0']
    pixels = list(tex.pixels)
    n = len(pixels)
    for i in range(0, n, 4):
        r, g, b, a = pixels[i:i+4]
        # Ignore background
        if r > 0.35 and g > 0.35:
            warmth = r - b
            if warmth > 0.07:
                # Inner cavity: delicate warm natural peach pearl
                blend = min(1.0, (warmth - 0.07) / 0.10)
                # Soft apricot pearl
                tr, tg, tb = 0.96, 0.86, 0.76
                pixels[i] = r * (1 - blend) + tr * blend
                pixels[i+1] = g * (1 - blend) + tg * blend
                pixels[i+2] = b * (1 - blend) + tb * blend
            else:
                # Exterior shell: clean, smooth luminous porcelain white
                # Smooth out dark veins
                tr, tg, tb = 0.98, 0.97, 0.96
                pixels[i] = r * 0.2 + tr * 0.8
                pixels[i+1] = g * 0.2 + tg * 0.8
                pixels[i+2] = b * 0.2 + tb * 0.8
    tex.pixels = pixels

# 2. Update Roughness texture (smooth glossy shell)
if 'Image_1' in bpy.data.images:
    tex_r = bpy.data.images['Image_1']
    r_pixels = list(tex_r.pixels)
    for i in range(0, len(r_pixels), 4):
        # Green channel is roughness in glTF (set to ~0.24 for porcelain sheen)
        r_pixels[i+1] = 0.24
    tex_r.pixels = r_pixels

# 3. Update BSDF
for mat in bpy.data.materials:
    bsdf = mat.node_tree.nodes.get("Principled BSDF")
    if bsdf:
        if "Roughness" in bsdf.inputs and not bsdf.inputs["Roughness"].is_linked:
            bsdf.inputs["Roughness"].default_value = 0.24
        if "Metallic" in bsdf.inputs and not bsdf.inputs["Metallic"].is_linked:
            bsdf.inputs["Metallic"].default_value = 0.0

out_path = 'android/app/src/main/assets/shrine/shankh/classic.glb'
bpy.ops.export_scene.gltf(filepath=out_path, export_format='GLB')
print("Successfully enhanced shankh model to", out_path)

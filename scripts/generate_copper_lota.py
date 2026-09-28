"""Generate the self-contained copper lota used by the Android Abhishek animation.

The lathed profile follows the supplied reference: rounded body, narrow neck,
wide flared mouth, rolled rim and a dark recessed interior. Coordinates are
Y-up glTF metres; SceneView scales the model to its ritual size.
"""
from __future__ import annotations

import json
import math
import pathlib
import struct

ROOT = pathlib.Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "android/app/src/main/assets/shrine/accessories/copper_lota.glb"
SIDES = 96

# (height, radius) pairs travel from the outer base, over the rim, then down
# the visible inner wall to the bottom of the opening.
PROFILE = [
    (0.012, .225), (.017, .270), (.035, .325), (.075, .385),
    (.130, .440), (.210, .480), (.295, .500), (.380, .490),
    (.455, .450), (.515, .397), (.560, .360), (.593, .350),
    (.620, .365), (.648, .410), (.678, .475), (.705, .550),
    (.717, .585), (.724, .590), (.731, .581), (.729, .565),
    (.713, .535), (.686, .480), (.660, .428), (.632, .383),
    (.604, .355), (.575, .337), (.530, .335),
]

# Slightly warmer outer body, dark copper interior, and fine turned lines.
MATERIALS = [
    ("Polished warm copper", [.62, .22, .105, 1], .75, .25),
    ("Copper interior", [.26, .090, .036, 1], .72, .34),
    ("Turned copper lines", [.49, .175, .067, 1], .78, .28),
    ("Water within lota", [.095, .135, .130, 1], .15, .17),
]

positions: list[float] = []
normals: list[float] = []
indices: list[list[int]] = [[] for _ in MATERIALS]


def vertex(x: float, y: float, z: float, nx: float, ny: float, nz: float) -> int:
    length = math.sqrt(nx * nx + ny * ny + nz * nz) or 1
    positions.extend((x, y, z))
    normals.extend((nx / length, ny / length, nz / length))
    return len(positions) // 3 - 1


def surface(profile: list[tuple[float, float]], material: int, inward: bool = False) -> None:
    start = len(positions) // 3
    for i, (y, radius) in enumerate(profile):
        lower = profile[max(i - 1, 0)]
        upper = profile[min(i + 1, len(profile) - 1)]
        dr = upper[1] - lower[1]
        dy = upper[0] - lower[0]
        for side in range(SIDES):
            angle = 2 * math.pi * side / SIDES
            c, s = math.cos(angle), math.sin(angle)
            # Meridian tangent is (dr, dy); its outward normal is (dy, -dr).
            flip = -1 if inward else 1
            vertex(radius * c, y, radius * s, flip * dy * c, -flip * dr, flip * dy * s)
    for ring in range(len(profile) - 1):
        for side in range(SIDES):
            a = start + ring * SIDES + side
            b = start + ring * SIDES + (side + 1) % SIDES
            c = a + SIDES
            d = b + SIDES
            triangles = (a, b, c, b, d, c) if inward else (a, c, b, b, c, d)
            indices[material].extend(triangles)


surface(PROFILE[:20], 0)
surface(PROFILE[19:], 1)
# Close the underside with a solid copper disk. The lathed body alone leaves
# an open ring, which becomes visible when the lota tips toward the deity.
base_y, base_radius = PROFILE[0]
base_center = vertex(0, base_y, 0, 0, -1, 0)
base_ring = [vertex(base_radius * math.cos(2 * math.pi * i / SIDES), base_y,
                    base_radius * math.sin(2 * math.pi * i / SIDES), 0, -1, 0)
             for i in range(SIDES)]
for i in range(SIDES):
    indices[0].extend((base_center, base_ring[i], base_ring[(i + 1) % SIDES]))
# Three very narrow engraved circles, like those visible on the supplied pot.
for y, radius in [(.201, .479), (.253, .494), (.467, .441), (.479, .432)]:
    surface([(y, radius + .0014), (y + .0025, radius + .0014)], 2)
# Recessed, horizontal water surface; it stays inside the neck when tilted.
water_y, water_radius = .548, .332
center = vertex(0, water_y, 0, 0, 1, 0)
ring = [vertex(water_radius * math.cos(2 * math.pi * i / SIDES), water_y,
               water_radius * math.sin(2 * math.pi * i / SIDES), 0, 1, 0)
        for i in range(SIDES)]
for i in range(SIDES):
    indices[3].extend((center, ring[(i + 1) % SIDES], ring[i]))

binary = bytearray()
views = []
accessors = []


def add(data: bytes, target: int) -> int:
    while len(binary) % 4:
        binary.append(0)
    offset = len(binary)
    binary.extend(data)
    views.append({"buffer": 0, "byteOffset": offset, "byteLength": len(data), "target": target})
    return len(views) - 1


pos_view = add(struct.pack(f"<{len(positions)}f", *positions), 34962)
norm_view = add(struct.pack(f"<{len(normals)}f", *normals), 34962)
for view, values in ((pos_view, positions), (norm_view, normals)):
    triples = list(zip(values[::3], values[1::3], values[2::3]))
    accessors.append({"bufferView": view, "componentType": 5126, "count": len(triples),
                      "type": "VEC3", "min": [min(v[i] for v in triples) for i in range(3)],
                      "max": [max(v[i] for v in triples) for i in range(3)]})

primitives = []
for material, faces in enumerate(indices):
    view = add(struct.pack(f"<{len(faces)}H", *faces), 34963)
    accessor = len(accessors)
    accessors.append({"bufferView": view, "componentType": 5123,
                      "count": len(faces), "type": "SCALAR",
                      "min": [min(faces)], "max": [max(faces)]})
    primitives.append({"attributes": {"POSITION": 0, "NORMAL": 1},
                       "indices": accessor, "material": material, "mode": 4})

model = {
    "asset": {"version": "2.0", "generator": "generate_copper_lota.py"},
    "scene": 0, "scenes": [{"nodes": [0]}], "nodes": [{"mesh": 0, "name": "Copper lota"}],
    "meshes": [{"name": "Copper lota for Abhishek", "primitives": primitives}],
    "materials": [{"name": name, "doubleSided": True,
                   "pbrMetallicRoughness": {"baseColorFactor": color,
                                            "metallicFactor": metallic,
                                            "roughnessFactor": roughness}}
                  for name, color, metallic, roughness in MATERIALS],
    "buffers": [{"byteLength": len(binary)}], "bufferViews": views,
    "accessors": accessors,
}
encoded = json.dumps(model, separators=(",", ":")).encode()
encoded += b" " * (-len(encoded) % 4)
binary.extend(b"\0" * (-len(binary) % 4))
OUTPUT.parent.mkdir(parents=True, exist_ok=True)
OUTPUT.write_bytes(struct.pack("<4sII", b"glTF", 2, 12 + 8 + len(encoded) + 8 + len(binary))
                   + struct.pack("<I4s", len(encoded), b"JSON") + encoded
                   + struct.pack("<I4s", len(binary), b"BIN\0") + binary)
print(OUTPUT)

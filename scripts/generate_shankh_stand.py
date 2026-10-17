"""Generate the sacred brass shankh holder (शंख आसन) for Pavitra Mandir.

Creates a traditional Indian temple brass stand (शंख आसन / त्रिपाद स्टैंड):
- An open oval brass cradle with a fluted fan supporting the bulbous body of the conch shell.
- A forward support arm gracefully supporting the underside of the neck.
- Arched brass tripod cabriole legs elevating the cradle above the floor.
- Uses identical node hierarchy and balanced bounding-box extents matching classic.glb,
  preserving the shell-relative placement in SceneView.
- Rich metallic temple brass PBR material (#D8AF52, metallic=0.88, roughness=0.22).
"""
import bpy
import bmesh
import math
import shutil
from mathutils import Vector

bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.gltf(filepath='android/app/src/main/assets/shrine/shankh/classic.glb')

classic_obj = [o for o in bpy.context.scene.objects if o.type == 'MESH'][0]
shell_0 = classic_obj.parent
M_inv = classic_obj.matrix_world.inverted()

local_bbox = [Vector(b) for b in classic_obj.bound_box]
min_local = Vector((min(v[i] for v in local_bbox) for i in range(3)))
max_local = Vector((max(v[i] for v in local_bbox) for i in range(3)))
c_center = (min_local + max_local) / 2.0

def make_brass_material():
    mat = bpy.data.materials.new(name='Temple_Brass')
    mat.use_nodes = True
    bsdf = mat.node_tree.nodes.get('Principled BSDF')
    if bsdf:
        bsdf.inputs['Base Color'].default_value = (0.847, 0.686, 0.322, 1.0)
        bsdf.inputs['Metallic'].default_value = 0.88
        bsdf.inputs['Roughness'].default_value = 0.22
    return mat

brass_mat = make_brass_material()

bm = bmesh.new()

def add_vert_world(w_pos):
    return bm.verts.new(M_inv @ Vector(w_pos))

# Open oval cradle, like the supplied brass holder. The central opening
# remains empty when the shell is lifted; only a narrow cast-metal rim supports it.
num_sectors = 96
rings = []
for rx, ry, z in [(.66,.49,-.79),(.94,.75,-.74),(.94,.75,-.795),(.66,.49,-.845)]:
    row=[]
    for i in range(num_sectors):
        angle=2*math.pi*i/num_sectors
        row.append(add_vert_world((.10+rx*math.cos(angle),ry*math.sin(angle),z)))
    rings.append(row)
for r in range(4):
    for i in range(num_sectors):
        j=(i+1)%num_sectors
        bm.faces.new([rings[r][i],rings[r][j],rings[(r+1)%4][j],rings[(r+1)%4][i]])

# Fluted fan at the broad end, with shallow radial ridges and a scalloped edge.
fan_rows=[]
for j in range(5):
    t=j/4
    row=[]
    for i in range(49):
        a=math.pi*.68 + math.pi*.64*i/48
        flute=.025*(.5+.5*math.cos(i*math.pi/2))*t
        rx=.90+.34*t; ry=.72+.12*t
        row.append(add_vert_world((.1+rx*math.cos(a),ry*math.sin(a),-.74+.11*t+flute)))
    fan_rows.append(row)
for j in range(4):
    for i in range(48):
        bm.faces.new([fan_rows[j][i],fan_rows[j+1][i],fan_rows[j+1][i+1],fan_rows[j][i+1]])

# A tapered tongue supports the narrow end of the conch.
prev=None
for i in range(25):
    t=i/24
    x=.80+1.52*t
    z=-.76+.55*t
    w=.23*(1-.50*t)+.035*math.sin(math.pi*t)
    ring=[add_vert_world((x,-w,z)),add_vert_world((x,w,z)),
          add_vert_world((x,w,z-.055)),add_vert_world((x,-w,z-.055))]
    if prev:
        for j in range(4):bm.faces.new([prev[j],ring[j],ring[(j+1)%4],prev[(j+1)%4]])
    else:bm.faces.new(list(reversed(ring)))
    prev=ring
bm.faces.new(prev)

# 3. Tripod legs
def make_cabriole_leg(bm, p_top, p_bot, r_top=0.032, r_bot=0.026):
    num_steps = 10
    num_sides = 8
    prev_ring = None
    for s in range(num_steps + 1):
        t = s / num_steps
        arch = math.sin(t * math.pi) * 0.04
        dx = p_bot[0] - p_top[0]
        dy = p_bot[1] - p_top[1]
        length_xy = math.hypot(dx, dy)
        norm_x = (dx / length_xy) if length_xy > 0 else 0
        norm_y = (dy / length_xy) if length_xy > 0 else 0

        x = p_top[0] + dx * t + norm_x * arch
        y = p_top[1] + dy * t + norm_y * arch
        z = p_top[2] + (p_bot[2] - p_top[2]) * t
        r = r_top + (r_bot - r_top) * t
        if s == num_steps:
            r *= 1.4 # Ornamental foot

        ring = []
        for a in range(num_sides):
            ang = 2 * math.pi * a / num_sides
            ring.append(add_vert_world((x + r * math.cos(ang), y + r * math.sin(ang), z)))
        if prev_ring:
            for a in range(num_sides):
                next_a = (a + 1) % num_sides
                bm.faces.new([prev_ring[a], ring[a], ring[next_a], prev_ring[next_a]])
        prev_ring = ring
    bm.faces.new(prev_ring)

legs = [
    ((-.35, -.56, -.79), (-.48, -.70, -1.36)), # Rear-Left leg
    ((-.35, .56, -.79), (-.48, .70, -1.36)), # Rear-Right leg
    ((1.80, 0.00, -.45), (1.95, 0.00, -1.36)), # Front leg
]

for p_top, p_bot in legs:
    make_cabriole_leg(bm, p_top, p_bot)

# Exact center balancing anchor:
# We know max local X in stand is 1.36000037
s_max_0 = 1.36000037
s_min_0 = 2.0 * c_center[0] - s_max_0 # -1.631869135

target_min = Vector((s_min_0, min_local[1], min_local[2]))
target_max = Vector((s_max_0, max_local[1], max_local[2]))

va1 = bm.verts.new(target_min)
va2 = bm.verts.new(target_min + Vector((0.0001, 0, 0)))
va3 = bm.verts.new(target_min + Vector((0, 0.0001, 0)))
bm.faces.new([va1, va2, va3])

va4 = bm.verts.new(target_max)
va5 = bm.verts.new(target_max - Vector((0.0001, 0, 0)))
va6 = bm.verts.new(target_max - Vector((0, 0.0001, 0)))
bm.faces.new([va4, va5, va6])

bmesh.ops.recalc_face_normals(bm, faces=list(bm.faces))
mesh = bpy.data.meshes.new('ShankhStandMesh')
bm.to_mesh(mesh)
bm.free()

mesh.materials.append(brass_mat)
stand_obj = bpy.data.objects.new('Shankh_Stand', mesh)
bpy.context.scene.collection.objects.link(stand_obj)
stand_obj.parent = shell_0
stand_obj.matrix_local = classic_obj.matrix_local.copy()

for poly in mesh.polygons:
    poly.use_smooth = True

bpy.data.objects.remove(classic_obj, do_unlink=True)

out_path = 'android/app/src/main/assets/shrine/accessories/shankh_stand.glb'
bpy.ops.export_scene.gltf(filepath=out_path, export_format='GLB')
print('Successfully generated shankh stand to', out_path)

source_path = 'temple_models/source/accessories/shankh_stand.glb'
shutil.copyfile(out_path, source_path)
print('Copied to', source_path)

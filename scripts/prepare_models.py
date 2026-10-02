"""Run with Blender --background --python scripts/prepare_models.py.
Non-destructive mobile derivatives: preserve source GLBs, simplify, pack textures.
"""
import bpy, bmesh, json, pathlib, math, os, subprocess, sys, struct
from mathutils import Vector
ROOT=pathlib.Path(__file__).resolve().parents[1]
ASSETS=ROOT/'android/app/src/main/assets/shrine'
MODELS={key:ROOT/value for key,value in json.loads((ROOT/'shared_assets/catalog/model-sources.json').read_text()).items()}
# Phone profile: retain silhouette detail on flowers, spend fewer triangles on
# small ritual props. Below-budget meshes are retained; textures are still resized.
TEXTURE_LIMIT=512
FACE_BUDGETS={"accessories/bell":2500,"accessories/copper_lota":4000,
 "flowers/peony":18000,"flowers/bouquet":18000}
# These scanned petal atlases showed colour bleeding at the uniform 512px budget.
TEXTURE_BUDGETS={"flowers/peony":768,"flowers/bouquet":768}
GENERATED="accessories/copper_lota"
if not os.environ.get("MODEL_ONLY") or os.environ["MODEL_ONLY"]==GENERATED:
 subprocess.run([sys.executable,str(ROOT/'scripts/generate_copper_lota.py')],check=True)
MODELS[GENERATED]=ASSETS/(GENERATED+'.glb')

report=json.loads((ROOT/"shared_assets/catalog/model-report.json").read_text()) if os.environ.get("MODEL_ONLY") else []
for name,source in MODELS.items():
 if os.environ.get('MODEL_ONLY') and name!=os.environ['MODEL_ONLY']:continue
 bpy.ops.wm.read_factory_settings(use_empty=True)
 src=source; out=ASSETS/(name+'.glb');out.parent.mkdir(parents=True,exist_ok=True)
 bpy.ops.import_scene.gltf(filepath=str(src))
 meshes=[o for o in bpy.context.scene.objects if o.type=='MESH']
 if name=='flowers/azalea':
  # The source includes a detached colour calibration cube, not a flower.
  for o in list(meshes):
   if len(o.data.vertices)==24:
    bpy.data.objects.remove(o,do_unlink=True);meshes.remove(o)
 faces=sum(len(o.data.polygons) for o in meshes)
 # Scanned GLBs often have disconnected triangles at UV seams. Weld geometry
 # before decimation, otherwise decimation removes isolated faces and leaves holes.
 if name.startswith('flowers/'):
  bpy.ops.object.select_all(action='DESELECT')
  for o in meshes:o.select_set(True)
  bpy.context.view_layer.objects.active=meshes[0]
  bpy.ops.object.join()
  meshes=[bpy.context.object]
 for o in meshes:
  bm=bmesh.new();bm.from_mesh(o.data)
  extent=max(max(v.co[i] for v in bm.verts)-min(v.co[i] for v in bm.verts) for i in range(3))
  bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=max(extent*0.000001,0.00000001))
  bm.to_mesh(o.data);bm.free()
 budget=FACE_BUDGETS.get(name,12000 if name.startswith('flowers/') else 8000)
 ratio=min(1,budget/max(faces,1))
 for o in meshes:
  bpy.context.view_layer.objects.active=o
  if ratio<1 and len(o.data.polygons)>200:
   mod=o.modifiers.new('Mobile simplification','DECIMATE');mod.ratio=ratio
   bpy.ops.object.modifier_apply(modifier=mod.name)
 texture_limit=TEXTURE_BUDGETS.get(name,TEXTURE_LIMIT)
 for im in bpy.data.images:
  if im.size[0] and max(im.size)>texture_limit:
   factor=texture_limit/max(im.size);im.scale(max(1,int(im.size[0]*factor)),max(1,int(im.size[1]*factor)))
 bpy.ops.export_scene.gltf(filepath=str(out),export_format='GLB',export_image_format='AUTO',export_cameras=False,export_lights=False)
 # Deterministic model thumbnails using the exported object's native coordinates.
 points=[o.matrix_world@Vector(v) for o in meshes for v in o.bound_box]
 lo=Vector(tuple(min(p[i] for p in points) for i in range(3)));hi=Vector(tuple(max(p[i] for p in points) for i in range(3)))
 center=(lo+hi)/2;size=max(hi-lo)
 bpy.ops.object.camera_add(location=center+Vector((0,-2*size,1.3*size)))
 camera=bpy.context.object;camera.rotation_euler=(center-camera.location).to_track_quat('-Z','Y').to_euler();camera.data.type='ORTHO';camera.data.ortho_scale=size*1.35;bpy.context.scene.camera=camera
 for offset,power in [((1,-2,3),1000),((-2,-1,1),500)]:
  bpy.ops.object.light_add(type='AREA',location=center+Vector(offset)*size)
  light=bpy.context.object;light.data.energy=power*size*size;light.data.shape='DISK';light.data.size=size*3;light.rotation_euler=(center-light.location).to_track_quat('-Z','Y').to_euler()
 scene=bpy.context.scene;scene.render.engine='CYCLES';scene.cycles.samples=32;scene.cycles.use_denoising=True
 scene.render.resolution_x=256;scene.render.resolution_y=256;scene.render.resolution_percentage=100;scene.render.film_transparent=True
 scene.render.filepath=str(ASSETS/(name+'.png'))
 if name!=GENERATED: bpy.ops.render.render(write_still=True) # No picker thumbnail for the ritual-only lota.
 report=[r for r in report if r["id"]!=name]
 data=out.read_bytes();json_length=struct.unpack_from('<I',data,12)[0]
 gltf=json.loads(data[20:20+json_length])
 triangle_count=sum(gltf['accessors'][p['indices']]['count']//3 for m in gltf['meshes'] for p in m['primitives'])
 report_source=ROOT/'scripts/generate_copper_lota.py' if name==GENERATED else src
 report.append(dict(id=name,source=str(report_source.relative_to(ROOT)),runtime=str(out.relative_to(ROOT)),
  source_kind='procedural' if name==GENERATED else 'glb',source_bytes=report_source.stat().st_size,
  runtime_bytes=out.stat().st_size,source_faces=faces,runtime_faces=triangle_count,
  target_faces=budget,texture_limit=texture_limit))
 (ROOT/'shared_assets/catalog/model-report.json').write_text(json.dumps(report,indent=2))
 print('PREPARED',name,flush=True)

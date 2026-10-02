"""Validate catalog synchronization and GLB structure without third-party modules."""
import json,pathlib,struct,hashlib
from audit_models import inspect
root=pathlib.Path(__file__).resolve().parents[1]
source=root/'shared_assets/catalog/catalog.json';catalog=json.loads(source.read_text())
for target in ['backend/internal/community/catalog.json','android/app/src/main/assets/shrine/catalog.json']:
 assert source.read_bytes()==(root/target).read_bytes(),f'Catalog differs: {target}'
assets=root/'android/app/src/main/assets'
for category in catalog['categories']:
 ids=[o['id'] for o in category['options']]
 assert len(ids)==len(set(ids)) and 'original' in ids
 for option in category['options']:
  for key in ['path','thumbnail']:
   p=assets/option[key];assert p.is_file(),p
   assert p.resolve().is_relative_to(assets.resolve())
for p in (assets/'shrine').rglob('*.glb'):
 data=p.read_bytes();magic,version,length=struct.unpack('<III',data[:12])
 assert magic==0x46546c67 and version==2 and length==len(data),p
 size,kind=struct.unpack('<II',data[12:20]);assert kind==0x4e4f534a
 model=json.loads(data[20:20+size]);assert model.get('meshes'),p
 assert all('uri' not in b for b in model.get('buffers',[])),p
 assert all('uri' not in image for image in model.get('images',[])),p
 assert not model.get('extensionsRequired'),f'Unsupported decoder dependency: {p}'
report=json.loads((root/'shared_assets/catalog/model-report.json').read_text())
assert {str(p.relative_to(root)) for p in (assets/'shrine').rglob('*.glb')}=={entry['runtime'] for entry in report},'Model report must cover every runtime GLB'
for entry in report:
 assert (root/entry['source']).exists(),entry
 assert (root/entry['runtime']).stat().st_size==entry['runtime_bytes'],entry
 stats=inspect(root/entry['runtime'])
 assert stats['triangles']==entry['runtime_faces'],entry
 assert stats['triangles']<=entry['target_faces'],entry
 assert all(max(image['width'],image['height'])<=entry['texture_limit'] for image in stats['images']),entry
print(f"Verified {len(catalog['categories'])} categories, {sum(len(c['options']) for c in catalog['categories'])} choices and all embedded models.")

"""One-time, idempotent source migration. Never alters source asset bytes."""
import pathlib,json,shutil
root=pathlib.Path(__file__).resolve().parents[1]
report=json.loads((root/'shared_assets/catalog/model-report.json').read_text())
manifest=[]
for item in report:
 old=root/item['source'];new=root/'temple_models/source'/(item['id']+'.glb')
 new.parent.mkdir(parents=True,exist_ok=True)
 if old.exists() and old!=new and not new.exists():shutil.move(old,new)
 manifest.append(dict(original=item['source'],source=str(new.relative_to(root))))
 item['source']=str(new.relative_to(root))
images={'idols/images.jpeg':'idols/ganesh_hanuman.jpeg','idols/images (1).jpeg':'idols/shiva.jpeg','idols/images (2).jpeg':'idols/lakshmi.jpeg','idols/images (3).jpeg':'idols/durga.jpeg','idols/images (4).jpeg':'idols/ram_darbar.jpeg','shrine/images.jpeg':'backgrounds/marble.jpeg','shrine/images1.jpeg':'backgrounds/ivory.jpeg','shrine/images2.jpeg':'backgrounds/carved.jpeg','prashad/images (5).jpeg':'prasad/halwa.jpeg','laddu_bowl.jpeg':'prasad/laddu_bowl.jpeg','laddu_bowl.png':'prasad/laddu_bowl.png','idols/imgres.html':'references/idol_search.html'}
for old_name,new_name in images.items():
 old=root/'temple_models'/old_name;new=root/'temple_models/source'/new_name
 new.parent.mkdir(parents=True,exist_ok=True)
 if old.exists() and not new.exists():shutil.move(old,new)
 manifest.append(dict(original=str(old.relative_to(root)),source=str(new.relative_to(root))))
for p in (root/'temple_models/legacy_android').glob('*'):
 new=root/'temple_models/source/legacy'/p.name;new.parent.mkdir(parents=True,exist_ok=True)
 if not new.exists():shutil.move(p,new)
(root/'shared_assets/catalog/source-manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
(root/'shared_assets/catalog/model-report.json').write_text(json.dumps(report,indent=2)+'\n')
for p in sorted((root/'temple_models').rglob('*'),reverse=True):
 if p.is_dir() and not list(p.iterdir()):p.rmdir()

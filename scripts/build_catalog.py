"""Single source catalog -> Android assets and Go embedded catalog."""
import json,pathlib
root=pathlib.Path(__file__).resolve().parents[1]
categories=[]
def category(id,en,hi,options):
 categories.append(dict(id=id,label_en=en,label_hi=hi,options=[dict(id=i,label_en=e,label_hi=h,path=p,kind=k,thumbnail=t or p) for i,e,h,p,k,t in options]))
def model(id,en,hi,folder,name):return (id,en,hi,f'shrine/{folder}/{name}.glb','model',f'shrine/{folder}/{name}.png')
def picture(id,en,hi,folder,name):return (id,en,hi,f'shrine/{folder}/{name}.png','image',None)
category('shrine','Shrine','मंदिर',[
 picture('original','Wooden mandir','लकड़ी का मंदिर','backgrounds','wood'),
 picture('marble','Marble arch','संगमरमर मंदिर','backgrounds','marble'),picture('ivory','Ivory alcove','हाथीदाँत रंग का मंदिर','backgrounds','ivory'),picture('carved','Carved sanctuary','नक्काशीदार मंदिर','backgrounds','carved')])
category('idols','Idols','मूर्तियाँ',[
 picture('original','Ganesha & Lakshmi','गणेश जी और लक्ष्मी जी','idols','original'),
 picture('ganesh_hanuman','Ganesha & Hanuman','गणेश जी और हनुमान जी','idols','ganesh_hanuman'),
 picture('shiva','Shiva','शिव जी','idols','shiva'),picture('lakshmi','Lakshmi','लक्ष्मी जी','idols','lakshmi'),picture('durga','Durga','दुर्गा माँ','idols','durga'),picture('ram_darbar','Ram Darbar','राम दरबार','idols','ram_darbar')])
category('flowers','Flowers','फूल',[model('original','Sunflower & peony','सूरजमुखी और पियोनी','flowers','sunflower')]+[model(n,e,h,'flowers',n) for n,e,h in [('orchid','Orchid','ऑर्किड'),('rose','Rose','गुलाब'),('azalea','Azalea','अज़ेलिया'),('bouquet','Lily','लिली')]])
category('lamp','Oil lamp','दीपक',[model('original','Golden lamp','सुनहरा दीपक','lamps','golden_oil'),model('brass','Brass diya','पीतल का दीया','lamps','brass_diya')])
category('shankh','Shankh','शंख',[model('original','Classic shankh','पारंपरिक शंख','shankh','classic'),model('ivory','Ivory shankh','श्वेत शंख','shankh','ivory')])
category('aarti','Aarti lamp','आरती दीप',[model('original','Golden aarti','सुनहरी आरती','aarti','golden'),model('traditional','Traditional aarti','पारंपरिक आरती','aarti','traditional')])
category('prasad','Prasad','प्रसाद',[picture('original','Laddu bowl','लड्डू का कटोरा','prasad','laddu_bowl'),picture('halwa','Halwa','हलवा','prasad','halwa'),model('laddu','Tirupati laddu','तिरुपति लड्डू','prasad','laddu')])
data=dict(version=1,package_id='personal-mandir',purchase_available=False,asset_variants_available=True,streak_days=7,qualified_referrals=1,categories=categories)
for p in ['shared_assets/catalog/catalog.json','android/app/src/main/assets/shrine/catalog.json','backend/internal/community/catalog.json']:
 (root/p).parent.mkdir(parents=True,exist_ok=True);(root/p).write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')

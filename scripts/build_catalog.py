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
 picture('marble','Marble arch','संगमरमर मंदिर','backgrounds','marble'),
 picture('ivory','Ivory alcove','हाथीदाँत रंग का मंदिर','backgrounds','ivory'),
 picture('carved','Carved sanctuary','नक्काशीदार मंदिर','backgrounds','carved'),
 picture('swarna','Golden sanctum','स्वर्ण गर्भगृह मंदिर','backgrounds','swarna')])
category('idols','Idols','मूर्तियाँ',[
 picture('original','Ganesha & Lakshmi','गणेश जी और लक्ष्मी जी','idols','original'),
 picture('ganesh_hanuman','Ganesha & Hanuman','गणेश जी और हनुमान जी','idols','ganesh_hanuman'),
 picture('radha_krishna','Radha & Krishna','राधा कृष्ण','idols','radha_krishna'),
 picture('shiva','Shiva','शिव जी','idols','shiva'),
 picture('lakshmi','Lakshmi','लक्ष्मी जी','idols','lakshmi'),
 picture('durga','Durga','दुर्गा माँ','idols','durga'),
 picture('ram_darbar','Ram Darbar','राम दरबार','idols','ram_darbar'),
 picture('hanuman','Hanuman','हनुमान जी','idols','hanuman')])
category('garland','Garland','माला',[
 picture('original','Marigold garland','गेंदे की माला','garlands','marigold'),
 picture('rose','Rose & jasmine','गुलाब और चमेली की माला','garlands','rose_jasmine'),
 picture('lotus','Lotus & pearl','कमल और मोती की माला','garlands','lotus_pearl'),
 picture('mogra','Mogra & saffron','मोगरा और केसर माला','garlands','mogra')])
category('flowers','Flowers','फूल',[model('original','Sunflower & peony','सूरजमुखी और पियोनी','flowers','sunflower')]+[model(n,e,h,'flowers',n) for n,e,h in [('orchid','Orchid','ऑर्किड'),('rose','Rose','गुलाब'),('azalea','Azalea','अज़ेलिया'),('bouquet','Lily','लिली')]]+[picture('mogra_flower','Jasmine mogra','चमेली मोगरा','flowers','mogra_flower')])
category('incense','Dhoop & Agarbatti','धूप और अगरबत्ती',[
 picture('original','Agarbatti stand','अगरबत्ती स्टैंड','incense','agarbatti'),
 picture('dhoop','Loban dhoop dani','लोबान धूपदानी','incense','dhoop_dani'),
 picture('kamal_dhoop','Royal kamal dhoop','शाही कमल धूपदान','incense','kamal_dhoop'),
 picture('mayur_dhoop','Mayur dhoop stand','मोर धूप स्टैंड','incense','mayur_dhoop')])
category('lamp','Oil lamp','दीपक',[
 model('original','Single diya','एकमुखी दीपक','lamps','golden_oil'),
 model('brass','Panchmukhi diya','पंचमुखी दीपक','lamps','brass_diya'),
 picture('peacock','Mayur deepstambh','मोर दीपस्तंभ','lamps','peacock_lamp')])
category('shankh','Shankh','शंख',[
 model('original','Classic shankh','पारंपरिक शंख','shankh','classic'),
 model('ivory','Ivory shankh','श्वेत शंख','shankh','ivory'),
 picture('swarna','Swarna shankh','स्वर्ण नक्काशी शंख','shankh','swarna')])
category('aarti','Aarti lamp','आरती दीप',[
 model('original','Golden aarti','सुनहरी आरती','aarti','golden'),
 model('traditional','Traditional aarti','पारंपरिक आरती','aarti','traditional'),
 picture('karpura','Nagkanya panch-aarti','नागकन्या पंच-आरती','aarti','karpura')])
category('prasad','Prasad','प्रसाद',[
 picture('original','Laddu bowl','लड्डू का कटोरा','prasad','laddu_bowl'),
 picture('halwa','Halwa','हलवा','prasad','halwa'),
 model('laddu','Tirupati laddu','तिरुपति लड्डू','prasad','laddu'),
 picture('modak','Kesar modak','केसर मोदक','prasad','modak')])
data=dict(version=2,package_id='personal-mandir',purchase_available=False,asset_variants_available=True,daily_puja_coins=1,referral_install_coins=1,coin_costs=dict(shrine=7,garland=4,incense=3,flowers=2),free_categories=['idols'],categories=categories)
for p in ['shared_assets/catalog/catalog.json','backend/internal/community/catalog.json']:
 (root/p).parent.mkdir(parents=True,exist_ok=True);(root/p).write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
android_categories=[dict(c,options=[o for o in c['options'] if o['id']!='original']) if c['id']=='shrine' else c for c in categories]
android_data=dict(data,categories=android_categories)
p='android/app/src/main/assets/shrine/catalog.json'
(root/p).parent.mkdir(parents=True,exist_ok=True);(root/p).write_text(json.dumps(android_data,ensure_ascii=False,indent=2)+'\n')


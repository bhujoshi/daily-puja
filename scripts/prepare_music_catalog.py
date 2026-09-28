"""Curate Archive recordings with aliases, collection mining and audio checks.
python3 scripts/prepare_music_catalog.py scripts/music_requests.json output.json
Requires ffmpeg/ffprobe. Cached research lives in /tmp/music-curation, never the app.
No audio is rehosted; the catalog retains Archive URLs and source attribution.
"""
import concurrent.futures as futures
import difflib, functools, hashlib, json, re, subprocess, sys, threading, unicodedata
import urllib.parse, urllib.request
from pathlib import Path
CACHE=Path('/tmp/music-curation'); CACHE.mkdir(exist_ok=True)
def save_json(path,data):
 temp=path.with_suffix('.tmp-'+str(threading.get_ident()))
 temp.write_text(json.dumps(data,ensure_ascii=False));temp.replace(path)
BAD=re.compile(r'\b(?:8\s*d|remix|dj|sped|speed|super\s*fast|slowed|lofi|ringtone|shorts|lecture|pravachan|discourse|karaoke|instrumental|mashup|7\s*times|108\s*times)\b',re.I)
STOP={'ji','shri','sri','shree','ke','ki','ka','ko','hai','ho','o','main','me','se','to','a','the','bhajan','song','audio','mp3','official','video','full','hd','lyrics','lord','hindi','mein','mai','hain','में','के','की','का','को','से','जी','श्री','है'}

def fetch(url):
 p=CACHE/(hashlib.sha256(url.encode()).hexdigest()+'.json')
 if p.exists():return json.loads(p.read_text())
 req=urllib.request.Request(url,headers={'User-Agent':'PavitraMandir-Catalog/2.0'})
 with urllib.request.urlopen(req,timeout=25) as r:data=json.load(r)
 save_json(p,data);return data

def text(value):return ' '.join(map(str,value)) if isinstance(value,list) else str(value or '')
def norm(s):
 s=re.sub(r'(?<=[a-z])(?=[A-Z])',' ',s)
 s=unicodedata.normalize('NFKC',s).lower()
 for a,b in [('raam','ram'),('shiva','shiv'),('shivay','shivaya'),('pooja','puja'),('moriya','morya'),('ganapati','ganpati'),('ganapat','ganpat'),('namaha','namah'),('chala','chala'),('laal','lal'),('aan','an'),('ashtakam','ashtak'),('stotram','stotra'),('atharvashirsham','atharvashirsh'),('atharvashirsha','atharvashirsh'),('karpoora','karpur'),('gauram','gauram')]:s=s.replace(a,b)
 s=s.replace('aa','a').replace('ee','i').replace('oo','u').replace('v','w')
 return ''.join(c if c.isalnum() or c.isspace() or unicodedata.category(c).startswith('M') else ' ' for c in s)
@functools.lru_cache(maxsize=32768)
def words(s):return [w for w in norm(s).split() if w not in STOP and not w.isdigit()]
@functools.lru_cache(maxsize=131072)
def compare(w,v):
 if any('\u0900'<=c<='\u097f' for c in w+v):return float(w==v)
 return difflib.SequenceMatcher(None,w,v).ratio()

def similarity(title,label):
 a,b=words(title),words(label)
 if not a or not b:return 0
 # Require each meaningful title word, allowing transliteration variation.
 remaining=list(b);matches=[]
 for w in a:
  if not remaining:matches.append(0);continue
  best=max(range(len(remaining)),key=lambda i:compare(w,remaining[i]))
  matches.append(compare(w,remaining.pop(best)))
 coverage=sum(matches)/len(a)
 strong=sum(v>=.78 for v in matches)/len(a)
 if strong < .8:return 0
 return coverage

def candidates(track,pool):
 aliases=[track['title']]+track.get('aliases',[])
 result=[]
 for item,f in pool:
  label=f['name'] + ' ' + text(f.get('title'))
  if BAD.search(label) or BAD.search(item.get('title','')):continue
  score=max(similarity(a,label) for a in aliases)
  if score>=.86:
   bitrate=float(f.get('bitrate',0) or 0)
   result.append((score+(min(bitrate,256)/256)*.025,item,f))
 return sorted(result,key=lambda x:x[0],reverse=True)

def metadata(identifier):
 data=fetch('https://archive.org/metadata/'+urllib.parse.quote(identifier,safe=''))
 md=data.get('metadata',{})
 if data.get('is_dark') or md.get('access-restricted-item')=='true':return []
 item={'id':identifier,'title':text(md.get('title')),'artist':text(md.get('creator')),'licenseUrl':text(md.get('licenseurl'))}
 return [(item,f) for f in data.get('files',[]) if f.get('name','').lower().endswith('.mp3') and not f.get('private')]

def search(query,rows=12):
 params=urllib.parse.urlencode({'q':'('+query+') AND mediatype:(audio OR movies)','output':'json','rows':rows,'fl[]':['identifier','title'],'sort[]':'downloads desc'},doseq=True)
 try:return fetch('https://archive.org/advancedsearch.php?'+params)['response']['docs']
 except Exception:return []

def duration(s):
 try:
  parts=str(s).split(':');return sum(float(v)*60**i for i,v in enumerate(reversed(parts)))
 except (TypeError,ValueError):return 0

def validate(item,f):
 url='https://archive.org/download/'+urllib.parse.quote(item['id'],safe='')+'/'+urllib.parse.quote(f['name'],safe='')
 key=hashlib.sha256(url.encode()).hexdigest();saved=CACHE/(key+'.quality.json')
 if saved.exists():
  result=json.loads(saved.read_text());result['volumeGainDb']=max(-12,min(0,result.get('volumeGainDb',0)))
  if 'sourceCreator' not in result:result['sourceCreator']=result.pop('artist','');result['artist']=''
  return result
 sample=CACHE/(key+'.mp3')
 try:
  req=urllib.request.Request(url,headers={'Range':'bytes=0-1048575','User-Agent':'PavitraMandir-Catalog/2.0'})
  with urllib.request.urlopen(req,timeout=25) as r:
   if 'text/html' in r.headers.get('Content-Type',''):return None
   sample.write_bytes(r.read(1048576))
  probe=json.loads(subprocess.check_output(['ffprobe','-v','error','-show_format','-show_streams','-of','json',str(sample)],stderr=subprocess.DEVNULL,timeout=15))
  audio=next(s for s in probe['streams'] if s.get('codec_type')=='audio')
  bitrate=int(audio.get('bit_rate') or probe['format'].get('bit_rate') or 0)
  rate=int(audio.get('sample_rate',0))
  seconds=duration(f.get('length')) or float(probe['format'].get('duration',0))
  if not duration(f.get('length')) and int(f.get('size',0))>sample.stat().st_size and bitrate:seconds=int(f['size'])*8/bitrate
  if bitrate<112000 or rate<32000 or seconds<60 or seconds>2400:return None
  decoded=subprocess.run(['ffmpeg','-v','info','-i',str(sample),'-t','20','-af','volumedetect','-f','null','-'],capture_output=True,text=True,timeout=20)
  if decoded.returncode:return None
  mean=re.search(r'mean_volume: ([\-\d.]+) dB',decoded.stderr)
  peak=re.search(r'max_volume: ([\-\d.]+) dB',decoded.stderr)
  if not mean or float(mean[1]) < -40:return None
  # Attenuate unusually loud masters; never amplify a poor source.
  gain=max(-12,min(0,-18-float(mean[1])))
  result={'audioUrl':url,'durationSeconds':round(seconds),'bitrateKbps':round(bitrate/1000),'sampleRateHz':rate,'volumeGainDb':round(gain,1),'sampleMeanDb':float(mean[1]),'samplePeakDb':float(peak[1]) if peak else None,'qualityCheck':'20-second decode and loudness sample','sourceUrl':'https://archive.org/details/'+item['id'],'artist':text(probe.get('format',{}).get('tags',{}).get('artist')),'sourceCreator':item['artist'],'licenseUrl':item['licenseUrl']}
  save_json(saved,result);return result
 except Exception:return None

POOL=[]

def resolve(t):
 checked=set()
 def attempt(pool):
  for score,item,f in candidates(t,pool)[:8]:
   key=(item['id'],f['name'])
   if key in checked:continue
   checked.add(key)
   q=validate(item,f)
   if q:
    out={k:v for k,v in t.items() if k!='aliases'};out.update(q)
    print('FOUND',t['id'],t['title'],'=>',f['name'],q['bitrateKbps'],flush=True)
    return out
  return None
 found=attempt(POOL)
 if found:return found
 aliases=t.get('aliases',[t['title']])
 queries=['"'+a+'"' for a in aliases]
 # Broad phrases find recordings within album metadata as well as item titles.
 roman=[w for w in re.findall(r'[a-z]+',aliases[0].lower()) if w not in STOP];queries+=[' AND '.join('"'+w+'"' for w in roman[:3])]
 querywords=[w for w in roman if w not in {'tere','teri','mere','meri','hey','naam','nam','tu','tum','nahi','ram','shiv'}]
 if len(querywords)>=2:queries+=[' AND '.join('"'+w+'"' for w in querywords[:2])]
 queries=list(dict.fromkeys(queries))
 for query in queries:
  pool=[]
  for d in search(query):
   try:pool+=metadata(d['identifier'])
   except Exception:pass
  found=attempt(pool)
  if found:return found
 print('UNRESOLVED',t['id'],t['title'],flush=True)
 return None

if __name__=='__main__':
 requested=json.loads(Path(sys.argv[1]).read_text())['tracks']
 Path(sys.argv[2]).write_text(json.dumps({'tracks':[]}))
 existing=json.loads(Path('backend/internal/music/catalog.json').read_text())['tracks']
 seeds={'Bhajans_101','HindiBhajans-collection','BestKrishnaBhajan1-151','BeautifulHindiBhajans','HindiBhajans-HemantChauhanMp3'}|{t['sourceUrl'].rsplit('/',1)[-1] for t in existing}
 seeds|={d['identifier'] for d in search('title:(bhajans OR bhajan OR "shiv aarti" OR "hanuman bhajan" OR "mata bhajan" OR "ganesh aarti")',160)}
 def safe_metadata(id):
  try:return metadata(id)
  except Exception:return []
 with futures.ThreadPoolExecutor(max_workers=6) as executor:
  for pool in executor.map(safe_metadata,sorted(seeds)):
   POOL+=pool
 print('Collection pool:',len(POOL),'files',flush=True)
 found=[]
 with futures.ThreadPoolExecutor(max_workers=6) as executor:
  jobs=[executor.submit(resolve,t) for t in requested]
  for job in futures.as_completed(jobs):
   t=job.result()
   if t:
    found.append(t)
    Path(sys.argv[2]).write_text(json.dumps({'tracks':sorted(found,key=lambda x:x['order'])},ensure_ascii=False,indent=2)+'\n')
 print('Resolved',len(found),'of',len(requested),flush=True)

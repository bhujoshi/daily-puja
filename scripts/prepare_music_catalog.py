"""Resolve a supplied catalog against Archive metadata; retain only matched, reachable MP3s.
Usage: python3 scripts/prepare_music_catalog.py input.json output.json
"""
import concurrent.futures, json, re, sys, urllib.request, urllib.parse
from pathlib import Path

def fetch(url):
    for attempt in range(2):
        try:
            with urllib.request.urlopen(url, timeout=22) as r: return json.load(r)
        except Exception:
            if attempt: raise

def norm(s):
    s=re.sub(r'[^a-z0-9 ]',' ',s.lower())
    for a,b in [('shivay','shivaya'),('raam','ram'),('mein','me'),('meri','mere'),('moriya','morya'),('hain','hai'),('wala','wale'),('shankar','shankar')]: s=s.replace(a,b)
    return set(s.split())-{'mp3','shri','sri','ji','ke','ki','ka','ko','hai','ho','o','main','me','a','the','bhajan','song','audio'}
def score(a,b):
    a,b=norm(a),norm(b)
    return len(a&b)/max(1,len(a))
def resolve(t):
    title=t['title']
    words=list(dict.fromkeys(re.findall(r'[A-Za-z]+',title)))
    queries=['title:"'+' '.join(words[:3])+'"','title:"'+title+'"',' AND '.join('"'+w+'"' for w in words if w.lower() not in {'ji','ke','ki','ka','ko','hai','ho','o','main','mein','se','to'})]
    for q in queries:
      try:
        query=urllib.parse.urlencode({'q':'('+q+') AND mediatype:audio','output':'json','rows':5,'fl[]':['identifier','title'],'sort[]':'downloads desc'},doseq=True)
        docs=fetch('https://archive.org/advancedsearch.php?'+query)['response']['docs']
        for d in docs:
          meta=fetch('https://archive.org/metadata/'+urllib.parse.quote(d['identifier']))
          if meta.get('is_dark') or meta.get('metadata',{}).get('access-restricted-item')=='true': continue
          files=[f for f in meta.get('files',[]) if f['name'].lower().endswith('.mp3') and not f.get('private')]
          candidates=[]
          for f in files:
            label=f.get('title',f['name'])
            if isinstance(label,list): label=' '.join(label)
            match=score(title,label)
            if len(files)==1: match=max(match,score(title,str(d.get('title',''))))
            if any(x in str(label).lower() for x in ['pravachan','discourse','lecture','ringtone','shorts']): continue
            if match>=.8 and float(f.get('length', 180) or 180) >= 60: candidates.append((match,f))
          for _,f in sorted(candidates,key=lambda x:x[0],reverse=True)[:2]:
            url='https://archive.org/download/'+urllib.parse.quote(d['identifier'],safe='')+'/'+urllib.parse.quote(f['name'],safe='')
            try:
              req=urllib.request.Request(url,headers={'Range':'bytes=0-1023'})
              with urllib.request.urlopen(req,timeout=25) as r:
                content=r.read(1024)
                if not content or 'text/html' in r.headers.get('Content-Type',''): continue
              result=dict(t,audioUrl=url,artist=meta.get('metadata',{}).get('creator',''),sourceUrl='https://archive.org/details/'+d['identifier'])
              if isinstance(result['artist'],list): result['artist']=', '.join(result['artist'])
              print('FOUND',t['id'],title,'=>',f['name'],flush=True)
              return result
            except Exception: pass
      except Exception as e: print('RETRY/OMIT',t['id'],type(e).__name__,flush=True)
    print('OMIT',t['id'],title,flush=True)
    return None

if __name__=='__main__':
    tracks=json.loads(Path(sys.argv[1]).read_text())['tracks']
    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
      found=[t for t in pool.map(resolve,tracks) if t]
    Path(sys.argv[2]).write_text(json.dumps({'tracks':found},indent=2)+'\n')
    print('Resolved',len(found),'of',len(tracks),flush=True)

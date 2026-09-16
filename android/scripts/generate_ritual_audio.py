"""Original procedural instrumental cues; no downloaded or copyrighted recordings."""
import math, random, wave, struct
from pathlib import Path
RATE=22050
out=Path(__file__).resolve().parents[1]/'app/src/main/res/raw'
def save(name,duration,sample):
    with wave.open(str(out/(name+'.wav')),'wb') as f:
        f.setparams((1,2,RATE,0,'NONE','not compressed'))
        f.writeframes(b''.join(struct.pack('<h',int(max(-1,min(1,sample(i/RATE)))*28000)) for i in range(int(duration*RATE))))
def note(t,f):
    return math.sin(2*math.pi*f*t)+.24*math.sin(4*math.pi*f*t)+.08*math.sin(6*math.pi*f*t)
notes=[261.63,293.66,329.63,392,440,392,329.63,293.66]
def music(t):
    beat=t%1.5; index=int(t/1.5)%8
    env=(1-math.exp(-beat*24))*math.exp(-beat*1.6)
    drone=.045*(math.sin(2*math.pi*130.815*t)+math.sin(2*math.pi*196*t))
    return .16*env*note(beat,notes[index])+drone
save('ritual_music',12,music)
random.seed(17)
low=0
def water(t):
    global low
    low=.86*low+.14*random.uniform(-1,1)
    env=min(t*8,(2.8-t)*8,1)
    return env*(low*.7+.06*math.sin(2*math.pi*(720*t+10*math.sin(t*17))))
save('water_offering',2.8,water)
def offering(t):
    return sum(.12*math.exp(-(t-start)*3)*note(t-start,f) for start,f in [(0,523.25),(.18,659.25),(.36,783.99)] if t>=start)*min(1,t*100,(1.6-t)*20)
save('flower_offering',1.6,offering)

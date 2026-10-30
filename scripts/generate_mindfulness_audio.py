"""Generate original, offline flute-like meditation cues using only the standard library.

Smooth attacks and releases avoid a struck-bell transient. Keep generous headroom;
the longer cue marks session boundaries, and the short note accompanies mantra taps.
"""

import math
from pathlib import Path
import struct
import wave

RATE = 22050
OUTPUT = Path(__file__).resolve().parents[1] / "android/app/src/main/res/raw"


def render(name, duration, frequency, peak, attack, release):
    count = round(duration * RATE)
    samples = []
    for i in range(count):
        t = i / RATE
        # Raised-cosine fades begin and end at silence without abrupt edges.
        fade_in = 0.5 - 0.5 * math.cos(math.pi * min(t / attack, 1))
        fade_out = 0.5 - 0.5 * math.cos(math.pi * min((count - 1 - i) / RATE / release, 1))
        phase = 2 * math.pi * frequency * t + 0.025 * math.sin(2 * math.pi * 4.2 * t)
        tone = math.sin(phase) + 0.16 * math.sin(2 * phase) + 0.035 * math.sin(3 * phase)
        breath = 0.96 + 0.04 * math.sin(2 * math.pi * 0.65 * t)
        samples.append(tone * fade_in * fade_out * breath)
    scale = peak * 32767 / max(abs(value) for value in samples)
    pcm = [round(value * scale) for value in samples]
    assert pcm[0] == pcm[-1] == 0
    assert max(abs(value) for value in pcm) < 32767
    with wave.open(str(OUTPUT / name), "wb") as output:
        output.setparams((1, 2, RATE, count, "NONE", "not compressed"))
        output.writeframes(struct.pack(f"<{count}h", *pcm))
    print(f"{name}: {duration:.2f}s, peak {20 * math.log10(peak):.1f} dBFS")


if __name__ == "__main__":
    render("mindfulness_flute.wav", 3.6, 293.665, 0.32, 0.6, 1.8)
    render("mindfulness_note.wav", 0.65, 440.0, 0.18, 0.09, 0.5)

"""Assets for the chaos features: item and effect textures, the vignette, five synthesized sounds, recipes, lang."""
import json, os, subprocess, wave, math
import numpy as np
from PIL import Image, ImageDraw, ImageFilter
from scipy.signal import butter, lfilter

R = '/home/claude/freerun/src/main/resources'
A = f'{R}/assets/freerun'
D = f'{R}/data/freerun'
OUT = os.path.dirname(os.path.abspath(__file__))
rng = np.random.default_rng(5)
SR = 44100


def jw(p, o):
    os.makedirs(os.path.dirname(p), exist_ok=True)
    json.dump(o, open(p, 'w'), indent=1)


def save_tex(im, rel):
    p = f'{A}/textures/{rel}.png'
    os.makedirs(os.path.dirname(p), exist_ok=True)
    im.save(p)


def icon(draw, size=16, scale=8):
    big = Image.new('RGBA', (size * scale, size * scale), (0, 0, 0, 0))
    draw(ImageDraw.Draw(big), scale)
    return big.resize((size, size), Image.LANCZOS)


# ---------------- items
def can(d, s, label=True, color=(30, 30, 34, 255)):
    d.rounded_rectangle([4 * s, 1.5 * s, 12 * s, 15 * s], radius=int(1.2 * s), fill=color, outline=(10, 10, 12, 255), width=s // 2)
    d.rectangle([4.5 * s, 1.5 * s, 11.5 * s, 2.6 * s], fill=(190, 194, 200, 255))
    d.rectangle([4.5 * s, 13.8 * s, 11.5 * s, 15 * s], fill=(170, 174, 180, 255))
    if label:
        # a slashing green bolt and a stripe
        d.polygon([(9 * s, 3.5 * s), (6 * s, 8.5 * s), (8 * s, 8.5 * s), (6.5 * s, 12.8 * s), (10.2 * s, 7 * s), (8.2 * s, 7 * s)],
                  fill=(124, 255, 74, 255))
        d.rectangle([4.2 * s, 9.6 * s, 11.8 * s, 10.4 * s], fill=(124, 255, 74, 160))
    d.line([(10.6 * s, 3 * s), (10.6 * s, 13 * s)], fill=(255, 255, 255, 60), width=s // 2)

save_tex(icon(lambda d, s: can(d, s)), 'item/energy_drink')
save_tex(icon(lambda d, s: can(d, s, label=False, color=(150, 154, 160, 255))), 'item/empty_can')

def meatloaf(d, s):
    d.ellipse([1 * s, 9 * s, 15 * s, 15 * s], fill=(235, 235, 240, 255), outline=(150, 150, 160, 255), width=s // 2)   # plate
    d.rounded_rectangle([3 * s, 6 * s, 13 * s, 12 * s], radius=int(1.5 * s), fill=(122, 66, 40, 255), outline=(70, 36, 20, 255), width=s // 2)
    d.rectangle([3.5 * s, 6 * s, 12.5 * s, 7.6 * s], fill=(200, 50, 30, 255))   # ketchup glaze
    for x in (5, 7.5, 10):
        d.line([(x * s, 7.6 * s), (x * s, 11.5 * s)], fill=(95, 50, 30, 255), width=s // 2)
    d.ellipse([10 * s, 3 * s, 13 * s, 6 * s], fill=(255, 120, 160, 255))   # a little heart from Mom
save_tex(icon(meatloaf), 'item/moms_meatloaf')

def clock(d, s):
    d.ellipse([1 * s, 1 * s, 15 * s, 15 * s], fill=(120, 60, 180, 255), outline=(60, 20, 100, 255), width=s)
    d.ellipse([3 * s, 3 * s, 13 * s, 13 * s], fill=(236, 226, 250, 255))
    for k in range(12):
        a = k / 12 * math.tau
        d.point((8 * s + 4.2 * s * math.cos(a), 8 * s + 4.2 * s * math.sin(a)), fill=(60, 20, 100, 255))
    d.line([(8 * s, 8 * s), (5.5 * s, 5 * s)], fill=(30, 10, 60, 255), width=s)
    d.line([(8 * s, 8 * s), (11 * s, 9.5 * s)], fill=(30, 10, 60, 255), width=s)
    # a backwards arrow round the rim
    d.arc([1.5 * s, 1.5 * s, 14.5 * s, 14.5 * s], 200, 320, fill=(255, 200, 80, 255), width=s)
    d.polygon([(2 * s, 9 * s), (1 * s, 6 * s), (4 * s, 6.8 * s)], fill=(255, 200, 80, 255))
save_tex(icon(clock), 'item/backwards_clock')

for n in ('energy_drink', 'empty_can', 'moms_meatloaf', 'backwards_clock'):
    jw(f'{A}/models/item/{n}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'freerun:item/{n}'}})

# ---------------- effect icons (18x18)
def eff_locked(d, s):
    d.ellipse([2 * s, 2 * s, 16 * s, 16 * s], outline=(124, 255, 74, 255), width=s * 2)
    d.ellipse([7 * s, 7 * s, 11 * s, 11 * s], fill=(124, 255, 74, 255))
    for a in range(4):
        t = a * math.pi / 2
        d.line([(9 * s + 5 * s * math.cos(t), 9 * s + 5 * s * math.sin(t)), (9 * s + 8 * s * math.cos(t), 9 * s + 8 * s * math.sin(t))],
               fill=(124, 255, 74, 255), width=s)
def eff_fog(d, s):
    for i, (x, y, r) in enumerate(((6, 9, 4), (11, 8, 5), (9, 12, 4))):
        d.ellipse([(x - r) * s, (y - r) * s, (x + r) * s, (y + r) * s], fill=(190, 190, 205, 230))
    d.text((6 * s, 3 * s), "?", fill=(80, 80, 100, 255))
    d.line([(6 * s, 4 * s), (8 * s, 2 * s), (10 * s, 3 * s), (9 * s, 6 * s), (9 * s, 7 * s)], fill=(80, 80, 110, 255), width=s)
for n, f in (('locked_in', eff_locked), ('brain_fog', eff_fog)):
    save_tex(icon(f, 18, 8), f'mob_effect/{n}')

# ---------------- the vignette: white, clear in the middle, opaque at the edges
N = 256
yy, xx = np.mgrid[0:N, 0:N] / (N - 1) * 2 - 1
r = np.sqrt(xx ** 2 * 1.0 + yy ** 2 * 1.25)
a = np.clip((r - 0.42) / 0.75, 0, 1) ** 1.6
img = np.zeros((N, N, 4), np.uint8)
img[..., :3] = 255
img[..., 3] = (a * 255).astype(np.uint8)
save_tex(Image.fromarray(img, 'RGBA'), 'gui/chaos/vignette')

# ---------------- sounds
def lp(x, f, o=2):
    b, a = butter(o, f / (SR / 2), 'low'); return lfilter(b, a, x)
def hp(x, f, o=2):
    b, a = butter(o, f / (SR / 2), 'high'); return lfilter(b, a, x)
def bp(x, lo, hi):
    b, a = butter(2, [lo / (SR / 2), hi / (SR / 2)], 'band'); return lfilter(b, a, x)
def mtof(m):
    return 440 * 2 ** ((m - 69) / 12)
def env(n, a, d):
    t = np.arange(n) / SR
    return np.minimum(1, t / max(a, 1e-4)) * np.exp(-t / d)
def wsave(name, x, stereo=False):
    x = x / (np.max(np.abs(x)) + 1e-9) * 0.85
    wav = f'{OUT}/{name}.wav'
    with wave.open(wav, 'wb') as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes((x * 32767).astype(np.int16).tobytes())
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', wav, '-c:a', 'libvorbis', '-q:a', '5', f'{A}/sounds/{name}.ogg'], check=True)

def epiano(f, dur, vel=1.0):
    n = int(SR * dur); t = np.arange(n) / SR
    x = (np.sin(2 * np.pi * f * t) + 0.35 * np.sin(2 * np.pi * 2 * f * t + 0.3) * np.exp(-t / 0.4) + 0.12 * np.sin(2 * np.pi * 3.01 * f * t) * np.exp(-t / 0.2))
    x *= (1 + 0.15 * np.sin(2 * np.pi * 4.5 * t))   # tremolo
    return x * env(n, 0.006, dur * 0.55) * vel

# lo-fi beat: 84 bpm, Fmaj7 - Em7 - Dm7 - Cmaj7, two bars each, boom-bap, vinyl crackle, low-passed
bpm = 84; beat = 60 / bpm; bars = 8; L = int(SR * beat * 4 * bars)
mix = np.zeros(L)
chords = [[53, 57, 60, 64], [52, 55, 59, 62], [50, 53, 57, 60], [48, 52, 55, 59]]
for i, ch in enumerate(chords):
    for rep in range(2):
        start = int(SR * beat * 4 * (i * 2 + rep))
        for k, m in enumerate(ch):
            off = start + int(SR * 0.012 * k)   # a lazy strum
            note = epiano(mtof(m), beat * 3.8, 0.5)
            mix[off:off + len(note)] += note[:max(0, min(len(note), L - off))]
        # a little melody note on the off-beat
        mel = ch[-1] + 12 - (rep * 3)
        off = start + int(SR * beat * 2.5)
        note = epiano(mtof(mel), beat * 1.2, 0.35)
        mix[off:off + len(note)] += note[:max(0, min(len(note), L - off))]
# drums
def kick():
    n = int(SR * 0.35); t = np.arange(n) / SR
    return np.sin(2 * np.pi * (50 + 80 * np.exp(-t / 0.04)) * t) * env(n, 0.002, 0.12)
def snare():
    n = int(SR * 0.25)
    return (bp(rng.standard_normal(n), 1200, 6000) * 0.8 + np.sin(2 * np.pi * 190 * np.arange(n) / SR) * 0.4) * env(n, 0.001, 0.07)
def hat():
    n = int(SR * 0.06)
    return hp(rng.standard_normal(n), 7000) * env(n, 0.001, 0.015)
K, S, H = kick(), snare(), hat()
for b in range(bars * 4):
    t0 = int(SR * beat * b)
    swing = int(SR * beat * 0.08)
    for (pos, smp, vol) in ((0, K, 1.0 if b % 4 in (0,) else 0), (0, S, 0.7 if b % 4 in (1, 3) else 0), (int(SR * beat * 0.5) + swing, K, 0.6 if b % 4 == 2 else 0)):
        if vol:
            o = t0 + pos; mix[o:o + len(smp)] += smp[:max(0, min(len(smp), L - o))] * vol
    for e in range(2):
        o = t0 + int(SR * beat * 0.5 * e) + (swing if e else 0)
        mix[o:o + len(H)] += H[:max(0, min(len(H), L - o))] * 0.25
mix = lp(mix, 3200)
crackle = np.zeros(L); idx = rng.integers(0, L, 900); crackle[idx] = rng.uniform(-1, 1, 900)
mix += hp(crackle, 2000) * 0.25 + lp(rng.standard_normal(L), 600) * 0.012
wsave('lofi_beat', mix)

# phone ring: the classic two-burst ring, 0.4 on, 0.2 off, 0.4 on, 2 s silence (loops)
n = int(SR * 3.2); t = np.arange(n) / SR
ring = np.zeros(n)
for s0 in (0.0, 0.6):
    m = (t >= s0) & (t < s0 + 0.4)
    ring[m] = (np.sin(2 * np.pi * 440 * t[m]) + np.sin(2 * np.pi * 480 * t[m])) * (0.5 + 0.5 * np.sign(np.sin(2 * np.pi * 20 * t[m])))
wsave('phone_ring', lp(ring, 3000))

# sad piano: slow A minor arpeggios, lots of space
L = int(SR * 24); sad = np.zeros(L)
prog = [[57, 60, 64, 69], [53, 57, 60, 65], [48, 52, 55, 60], [52, 56, 59, 64]]
for i, ch in enumerate(prog):
    for k, m in enumerate(ch + [ch[2] + 12]):
        o = int(SR * (i * 6 + k * 0.9))
        f = mtof(m); nn = int(SR * 4); tt = np.arange(nn) / SR
        note = (np.sin(2 * np.pi * f * tt) + 0.3 * np.sin(2 * np.pi * 2 * f * tt) * np.exp(-tt / 0.8) + 0.1 * np.sin(2 * np.pi * 3 * f * tt) * np.exp(-tt / 0.3)) * env(nn, 0.004, 1.6) * 0.5
        sad[o:o + nn] += note[:max(0, min(nn, L - o))]
    bo = int(SR * i * 6); f = mtof(ch[0] - 12); nn = int(SR * 6); tt = np.arange(nn) / SR
    sad[bo:bo + nn] += np.sin(2 * np.pi * f * tt) * env(nn, 0.01, 2.5) * 0.35
# a touch of room
rev = np.zeros(L)
for dly, g in ((0.11, 0.3), (0.23, 0.2), (0.37, 0.12)):
    k = int(SR * dly); rev[k:] += sad[:-k] * g
wsave('sad_piano', lp(sad + rev, 5000))

# podcast jingle: a bright little synth riff
L = int(SR * 2.2); jj = np.zeros(L)
for i, m in enumerate([72, 76, 79, 84, 79, 84, 88]):
    o = int(SR * i * 0.16); nn = int(SR * 0.5); tt = np.arange(nn) / SR
    f = mtof(m)
    note = (np.sign(np.sin(2 * np.pi * f * tt)) * 0.3 + np.sin(2 * np.pi * f * tt)) * env(nn, 0.003, 0.15)
    jj[o:o + nn] += note[:max(0, min(nn, L - o))]
wsave('podcast_jingle', lp(jj, 6000))

# the crash: a sad trombone-ish slide down
n = int(SR * 1.8); t = np.arange(n) / SR
f = 330 * 2 ** (-t / 1.8 * 1.0)
ph = np.cumsum(2 * np.pi * f / SR)
tb = (np.sin(ph) + 0.5 * np.sin(2 * ph) + 0.3 * np.sin(3 * ph)) * (1 + 0.3 * np.sin(2 * np.pi * 6 * t)) * np.minimum(1, t / 0.05) * np.minimum(1, (1.8 - t) / 0.4)
wsave('energy_crash', lp(tb, 2500))

snd = json.load(open(f'{A}/sounds.json'))
for k, stream in (('lofi_beat', True), ('phone_ring', False), ('sad_piano', True), ('podcast_jingle', False), ('energy_crash', False)):
    e = {'name': f'freerun:{k}'}
    if stream: e['stream'] = True
    snd[k] = {'sounds': [e], 'subtitle': f'subtitles.freerun.{k}'}
json.dump(snd, open(f'{A}/sounds.json', 'w'), indent=2)

# ---------------- recipes
jw(f'{D}/recipe/energy_drink.json', {'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'ingredients': [
    {'item': 'minecraft:sugar'}, {'item': 'minecraft:sugar'}, {'item': 'minecraft:glowstone_dust'}, {'item': 'freerun:empty_can'}],
    'result': {'id': 'freerun:energy_drink', 'count': 1}})
jw(f'{D}/recipe/empty_can.json', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['I I', ' I '],
    'key': {'I': {'item': 'minecraft:iron_nugget'}}, 'result': {'id': 'freerun:empty_can', 'count': 2}})
jw(f'{D}/recipe/backwards_clock.json', {'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'ingredients': [
    {'item': 'minecraft:clock'}, {'item': 'minecraft:ender_pearl'}, {'item': 'minecraft:amethyst_shard'}],
    'result': {'id': 'freerun:backwards_clock', 'count': 1}})

lang = json.load(open(f'{A}/lang/en_us.json'))
lang.update({
    'item.freerun.energy_drink': 'LOCKD Energy', 'item.freerun.empty_can': 'Empty Can', 'item.freerun.moms_meatloaf': "Mom's Meatloaf",
    'item.freerun.backwards_clock': 'Backwards Clock', 'effect.freerun.locked_in': 'Locked In', 'effect.freerun.brain_fog': 'Brain Fog',
    'entity.freerun.homesick_dragon': 'The Ender Dragon (homesick)',
    'subtitles.freerun.lofi_beat': 'Lo-fi beat plays', 'subtitles.freerun.phone_ring': 'Phone rings', 'subtitles.freerun.sad_piano': 'Sad piano',
    'subtitles.freerun.podcast_jingle': 'Podcast intro', 'subtitles.freerun.energy_crash': 'You crash'})
json.dump(lang, open(f'{A}/lang/en_us.json', 'w'), indent=1, ensure_ascii=False)

# preview
prev = Image.new('RGBA', (16 * 6 * 6, 16 * 6), (60, 60, 60, 255))
for i, n in enumerate(['item/energy_drink', 'item/empty_can', 'item/moms_meatloaf', 'item/backwards_clock', 'mob_effect/locked_in', 'mob_effect/brain_fog']):
    im = Image.open(f'{A}/textures/{n}.png').resize((96, 96), Image.NEAREST)
    prev.paste(im, (i * 96, 0), im)
prev.save(f'{OUT}/../chaos_preview.png')
print('ok')

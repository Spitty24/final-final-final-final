"""Skateboard assets: synthesized sounds, item models, lang, recipes, sounds.json."""
import json, os, subprocess, wave
import numpy as np
from scipy.signal import butter, lfilter

R = '/home/claude/freerun/src/main/resources'
A = f'{R}/assets/freerun'
D = f'{R}/data/freerun'
SR = 44100
rng = np.random.default_rng(7)
OUT = os.path.dirname(os.path.abspath(__file__))


def jw(p, o):
    os.makedirs(os.path.dirname(p), exist_ok=True)
    json.dump(o, open(p, 'w'), indent=1)


def bp(x, lo, hi, order=2):
    b, a = butter(order, [lo / (SR / 2), hi / (SR / 2)], 'band')
    return lfilter(b, a, x)


def lp(x, hi, order=2):
    b, a = butter(order, hi / (SR / 2), 'low')
    return lfilter(b, a, x)


def env(n, attack, decay):
    t = np.arange(n) / SR
    e = np.minimum(1, t / max(attack, 1e-4)) * np.exp(-t / decay)
    return e


def t_(sec):
    return np.arange(int(SR * sec)) / SR


def save(name, x):
    x = x / (np.max(np.abs(x)) + 1e-9) * 0.9
    wav = f'{OUT}/{name}.wav'
    with wave.open(wav, 'wb') as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes((x * 32767).astype(np.int16).tobytes())
    os.makedirs(f'{A}/sounds', exist_ok=True)
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', wav, '-c:a', 'libvorbis', '-q:a', '5', f'{A}/sounds/{name}.ogg'], check=True)


def noise(n):
    return rng.standard_normal(n)


# rolling: low urethane rumble, a little bearing whine, and sidewalk cracks every ~0.5 s; loops cleanly
n = int(SR * 2.0)
t = np.arange(n) / SR
roll = lp(noise(n), 180, 3) * 1.4 + bp(noise(n), 300, 900) * 0.25 + 0.05 * np.sin(2 * np.pi * 1450 * t) * (1 + 0.3 * np.sin(2 * np.pi * 3 * t))
for c in (0.25, 0.75, 1.25, 1.75):
    i = int(c * SR)
    k = int(0.05 * SR)
    roll[i:i + k] += bp(noise(k), 80, 400) * env(k, 0.001, 0.012) * 3
fade = int(0.1 * SR)
roll[:fade] = roll[:fade] * np.linspace(0, 1, fade) + roll[-fade:] * np.linspace(1, 0, fade)
roll = roll[:-fade]
save('skate_roll', roll)

# pop: the tail cracking on the ground, then the wood
tt = t_(0.35)
crack = bp(noise(len(tt)), 1500, 7000) * env(len(tt), 0.0005, 0.012)
thump = np.sin(2 * np.pi * (140 - 60 * tt) * tt) * env(len(tt), 0.001, 0.05)
wood = (np.sin(2 * np.pi * 620 * tt) + 0.6 * np.sin(2 * np.pi * 980 * tt)) * env(len(tt), 0.001, 0.03)
save('skate_pop', crack * 1.2 + thump * 0.9 + wood * 0.5)

# land: all four wheels and the deck at once
tt = t_(0.5)
slap = bp(noise(len(tt)), 200, 3000) * env(len(tt), 0.001, 0.03)
body = np.sin(2 * np.pi * (95 - 30 * tt) * tt) * env(len(tt), 0.002, 0.09)
rattle = np.zeros(len(tt))
for d in (0.0, 0.012, 0.03):
    i = int(d * SR)
    k = int(0.06 * SR)
    rattle[i:i + k] += bp(noise(k), 2000, 6000) * env(k, 0.0005, 0.01)
save('skate_land', slap * 1.1 + body + rattle * 0.5)

# grind: steel trucks on a rail
tt = t_(0.9)
scrape = bp(noise(len(tt)), 2500, 8000, 3) * (0.7 + 0.3 * np.sin(2 * np.pi * 23 * tt))
ring = sum(np.sin(2 * np.pi * f * tt) * a for f, a in ((1830, 0.25), (2710, 0.18), (4120, 0.12)))
e = np.minimum(1, tt / 0.01) * np.minimum(1, (0.9 - tt) / 0.15)
save('skate_grind', (scrape + ring) * e)

# bail: a body hitting the floor and the board clattering away
tt = t_(1.0)
hit = lp(noise(len(tt)), 400) * env(len(tt), 0.002, 0.08) * 2 + np.sin(2 * np.pi * 70 * tt) * env(len(tt), 0.002, 0.12)
clat = np.zeros(len(tt))
for i, d in enumerate((0.1, 0.22, 0.31, 0.45, 0.52, 0.66)):
    s = int(d * SR)
    k = int(0.09 * SR)
    clat[s:s + k] += (bp(noise(k), 400, 2500) + np.sin(2 * np.pi * (500 + 70 * i) * np.arange(k) / SR)) * env(k, 0.0005, 0.03) * (1 - i * 0.12)
save('skate_bail', hit + clat * 0.8 + bp(noise(len(tt)), 100, 600) * np.exp(-tt / 0.5) * 0.2)

# push: a shoe on the ground
tt = t_(0.4)
scuff = bp(noise(len(tt)), 300, 2500) * np.sin(np.pi * np.clip(tt / 0.3, 0, 1)) ** 2
save('skate_push', scuff * 0.8)

snd = json.load(open(f'{A}/sounds.json'))
for k, dist in (('skate_roll', 16), ('skate_pop', 20), ('skate_land', 20), ('skate_grind', 24), ('skate_bail', 20), ('skate_push', 12)):
    snd[k] = {'sounds': [{'name': f'freerun:{k}', 'attenuation_distance': dist}], 'subtitle': f'subtitles.freerun.{k}'}
json.dump(snd, open(f'{A}/sounds.json', 'w'), indent=2)

# items: built-in renderer draws the 3D board
names = ['skateboard', 'skateboard_gator', 'skateboard_bolt', 'skateboard_crawfish']
decks = ['bayou', 'gator', 'bolt', 'crawfish']
for n_, d in zip(names, decks):
    jw(f'{A}/models/item/{n_}.json', {'parent': 'builtin/entity', 'gui_light': 'front',
        'textures': {'particle': f'freerun:entity/skateboard/{d}'},
        'display': {
            'gui': {'rotation': [30, 45, 0], 'translation': [0, 0, 0], 'scale': [1, 1, 1]},
            'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.6, 0.6, 0.6]},
            'fixed': {'rotation': [90, 0, 0], 'translation': [0, 0, 0], 'scale': [1, 1, 1]},
            'head': {'rotation': [0, 0, 0], 'translation': [0, 13, 0], 'scale': [1, 1, 1]},
            'thirdperson_righthand': {'rotation': [75, 0, 0], 'translation': [0, 2, 3], 'scale': [0.6, 0.6, 0.6]},
            'thirdperson_lefthand': {'rotation': [75, 0, 0], 'translation': [0, 2, 3], 'scale': [0.6, 0.6, 0.6]},
            'firstperson_righthand': {'rotation': [0, 0, 10], 'translation': [2, 0, 0], 'scale': [0.7, 0.7, 0.7]},
            'firstperson_lefthand': {'rotation': [0, 0, -10], 'translation': [2, 0, 0], 'scale': [0.7, 0.7, 0.7]}}})

# recipe: any planks for the deck, iron for the trucks, any dye-free wool for grip... keep it simple
key = {'P': {'tag': 'minecraft:planks'}, 'I': {'item': 'minecraft:iron_nugget'}, 'S': {'item': 'minecraft:slime_ball'}}
jw(f'{D}/recipe/skateboard.json', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['PPP', 'I I', 'S S'],
    'key': key, 'result': {'id': 'freerun:skateboard', 'count': 1}})
for n_, dye in (('skateboard_gator', 'minecraft:green_dye'), ('skateboard_bolt', 'minecraft:purple_dye'), ('skateboard_crawfish', 'minecraft:red_dye')):
    jw(f'{D}/recipe/{n_}.json', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
        'ingredients': [{'tag': 'freerun:skateboards'}, {'item': dye}], 'result': {'id': f'freerun:{n_}', 'count': 1}})
jw(f'{D}/tags/item/skateboards.json', {'values': [f'freerun:{n_}' for n_ in names]})

lang = json.load(open(f'{A}/lang/en_us.json'))
lang.update({
    'item.freerun.skateboard': 'Skateboard (Bayou)', 'item.freerun.skateboard_gator': 'Skateboard (Gator)',
    'item.freerun.skateboard_bolt': 'Skateboard (Bolt)', 'item.freerun.skateboard_crawfish': 'Skateboard (Crawfish)',
    'key.freerun.skate': 'Skateboard on / off',
    'subtitles.freerun.skate_roll': 'Wheels roll', 'subtitles.freerun.skate_pop': 'Board pops',
    'subtitles.freerun.skate_land': 'Board lands', 'subtitles.freerun.skate_grind': 'Trucks grind',
    'subtitles.freerun.skate_bail': 'Skater bails', 'subtitles.freerun.skate_push': 'Skater pushes'})
json.dump(lang, open(f'{A}/lang/en_us.json', 'w'), indent=1, ensure_ascii=False)
print('ok')

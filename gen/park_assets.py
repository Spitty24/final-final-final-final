"""Skate park + bus assets: textures, models, blockstates, loot, recipes, lang, sounds."""
import json, os, subprocess, wave, math
import numpy as np
from PIL import Image, ImageDraw, ImageFilter
from scipy.signal import butter, lfilter

R = '/home/claude/freerun/src/main/resources'
A = f'{R}/assets/freerun'
D = f'{R}/data/freerun'
OUT = os.path.dirname(os.path.abspath(__file__))
rng = np.random.default_rng(11)


def jw(p, o):
    os.makedirs(os.path.dirname(p), exist_ok=True)
    json.dump(o, open(p, 'w'), indent=1)


def save(img, rel):
    p = f'{A}/textures/{rel}.png'
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)


def noise_img(n, base, amp, blur=0):
    a = rng.normal(0, 1, (n, n))
    if blur:
        im = Image.fromarray(((a - a.min()) / (a.max() - a.min()) * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(blur))
        a = np.asarray(im).astype(float) / 255 - 0.5
        a /= max(1e-6, a.std())
    out = np.zeros((n, n, 4))
    for c in range(3):
        out[..., c] = base[c] + a * amp
    out[..., 3] = 255
    return out


def to_img(a):
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), 'RGBA')


S = 32
# ---------------- park concrete: smooth light grey, fine aggregate, faint trowel arcs
a = noise_img(S, (176, 178, 180), 5) + noise_img(S, (0, 0, 0), 4, blur=3)
for k in range(3):
    cx, cy, r = rng.uniform(0, S), rng.uniform(0, S), rng.uniform(10, 20)
    yy, xx = np.mgrid[0:S, 0:S]
    ring = np.abs(np.hypot(xx - cx, yy - cy) - r) < 0.7
    a[ring, :3] += 5
for _ in range(14):
    x, y = rng.integers(0, S, 2)
    a[y, x, :3] -= rng.uniform(10, 25)
a[0, :, :3] -= 10
a[:, 0, :3] -= 10
save(to_img(a), 'block/skatepark_concrete')

# ---------------- ramp surface: dark skatelite with speckle and a faint seam every half block
a = noise_img(S, (78, 74, 72), 4) + noise_img(S, (0, 0, 0), 3, blur=2)
for _ in range(40):
    x, y = rng.integers(0, S, 2)
    a[y, x, :3] += rng.uniform(8, 20)
a[S // 2, :, :3] -= 12
a[0, :, :3] -= 18
save(to_img(a), 'block/skate_ramp_top')

# ---------------- ramp side: painted plywood with frame studs and screws
img = Image.new('RGBA', (S, S), (0, 0, 0, 255))
pa = noise_img(S, (54, 92, 140), 5, blur=1)
for x in range(S):
    pa[:, x, :3] += 6 * math.sin(x * 0.7)
img = to_img(pa)
d = ImageDraw.Draw(img)
for x in (0, 15, 31):
    d.line([(x, 0), (x, S)], fill=(38, 66, 104, 255))
d.line([(0, S - 1), (S, S - 1)], fill=(30, 52, 84, 255))
d.line([(0, 0), (S, 0)], fill=(140, 110, 70, 255))   # ply edge showing at the top
for x in (3, 12, 19, 28):
    for y in (6, 18, 27):
        d.point((x, y), fill=(200, 200, 205, 255))
# a little graffiti tag
for i in range(10):
    t = i / 9
    d.point((6 + int(t * 18), 22 - int(6 * math.sin(t * math.pi))), fill=(240, 90, 170, 255))
    d.point((6 + int(t * 18), 23 - int(6 * math.sin(t * math.pi))), fill=(240, 90, 170, 255))
save(img, 'block/skate_ramp_side')

# ---------------- grind rail: brushed round steel, and the painted posts
a = np.zeros((16, 16, 4))
for y in range(16):
    shade = 150 + 70 * math.sin(y / 15 * math.pi) ** 2
    a[y, :, :3] = shade
    a[y, :, 3] = 255
a[..., :3] += rng.normal(0, 4, (16, 16, 1))
for x in range(16):
    a[:, x, :3] += 6 * ((x * 7) % 5 - 2) / 2
save(to_img(a), 'block/grind_rail')
a = noise_img(16, (196, 40, 36), 6)
a[::4, :, :3] -= 10
save(to_img(a), 'block/grind_rail_post')

# ---------------- bus stop sign: blue plate with a white bus and BUS
img = Image.new('RGBA', (32, 32), (24, 78, 168, 255))
d = ImageDraw.Draw(img)
d.rectangle([0, 0, 31, 31], outline=(240, 240, 240, 255))
d.rectangle([1, 1, 30, 30], outline=(18, 60, 130, 255))
# the bus
d.rectangle([6, 5, 25, 16], fill=(250, 250, 250, 255))
d.rectangle([8, 7, 23, 10], fill=(24, 78, 168, 255))
d.rectangle([9, 17, 11, 18], fill=(250, 250, 250, 255))
d.rectangle([20, 17, 22, 18], fill=(250, 250, 250, 255))
font = {'B': ["##.", "#.#", "##.", "#.#", "##."], 'U': ["#.#", "#.#", "#.#", "#.#", "###"], 'S': [".##", "#..", ".#.", "..#", "##."]}
x = 10
for ch in "BUS":
    for r, row in enumerate(font[ch]):
        for c, px in enumerate(row):
            if px == '#':
                d.point((x + c, 22 + r), fill=(255, 255, 255, 255))
    x += 4
save(img, 'block/bus_stop_sign')

# ---------------- kit icons
def icon(name, draw_fn, col):
    big = Image.new('RGBA', (128, 128), (0, 0, 0, 0))
    d = ImageDraw.Draw(big)
    # a cardboard box behind, then the feature's profile
    d.rounded_rectangle([6, 70, 122, 124], 8, fill=(176, 132, 82, 255), outline=(110, 78, 42, 255), width=4)
    d.line([(6, 86), (122, 86)], fill=(140, 100, 58, 255), width=3)
    draw_fn(d, col)
    im = big.resize((32, 32), Image.LANCZOS)
    save(im, f'item/kit_{name}')

wood, deck, steel, conc = (88, 120, 170, 255), (60, 60, 64, 255), (210, 214, 220, 255), (190, 192, 194, 255)
OUT_C = (30, 30, 36, 255)

def qp(d, c):
    pts = [(20 + 70 * math.sin(t / 20 * math.pi / 2), 34 + 70 * math.cos(t / 20 * math.pi / 2)) for t in range(21)] + [(110, 34), (110, 104)]
    d.polygon(pts, fill=wood, outline=OUT_C)
    d.rectangle([90, 30, 112, 36], fill=steel)

def hp(d, c):
    pts = [(8, 30)]
    for t in range(21):
        th = t / 20 * math.pi / 2
        pts.append((8 + 40 * (1 - math.cos(th)) * 0 + 40 * math.sin(th), 30 + 70 * (1 - math.cos(th + 0)) * 0 + 70 * math.sin(th) * 0 + 70 * (1 - math.cos(math.pi / 2 - th)) * 0 + 70 - 70 * math.cos(th)))
    left = [(8 + 40 * math.sin(t / 20 * math.pi / 2), 30 + 70 * math.sin(t / 20 * math.pi / 2) ** 0 * (1 - math.cos(t / 20 * math.pi / 2)) * 0 + 70 * (math.sin(t / 20 * math.pi / 2))) for t in range(21)]
    curve = [(8 + 40 * (1 - math.cos(t / 20 * math.pi / 2)) , 30 + 70 * math.sin(t / 20 * math.pi / 2)) for t in range(21)]
    curve = [(8 + 40 * math.sin(th), 100 - 70 * math.cos(th)) for th in [t / 20 * math.pi / 2 for t in range(21)]]
    # left wall: from top (8,30) curving down to the flat at (48,100)
    lw = [(8 + 40 * (1 - math.cos(th)), 30 + 70 * math.sin(th)) for th in [t / 20 * math.pi / 2 for t in range(21)]]
    rw = [(120 - 40 * (1 - math.cos(th)), 30 + 70 * math.sin(th)) for th in [t / 20 * math.pi / 2 for t in range(21)]]
    poly = [(4, 30), (4, 104)] + [(124, 104), (124, 30)] + rw + lw[::-1]
    d.polygon(poly, fill=wood, outline=OUT_C)

def kicker(d, c):
    d.polygon([(14, 104), (100, 104), (100, 50)], fill=wood, outline=OUT_C)
    d.line([(100, 50), (100, 104)], fill=steel, width=4)

def funbox(d, c):
    d.polygon([(8, 104), (36, 62), (92, 62), (120, 104)], fill=conc, outline=OUT_C)
    d.rectangle([40, 46, 88, 52], fill=steel, outline=OUT_C)
    d.rectangle([60, 52, 66, 62], fill=(196, 40, 36, 255))

def bank(d, c):
    d.polygon([(10, 104), (70, 60), (118, 60), (118, 104)], fill=conc, outline=OUT_C)

def rail(d, c):
    d.rectangle([10, 48, 118, 56], fill=steel, outline=OUT_C)
    for x in (24, 64, 104):
        d.rectangle([x - 4, 56, x + 4, 104], fill=(196, 40, 36, 255), outline=OUT_C)

def stairs(d, c):
    pts = [(8, 104), (8, 40), (40, 40), (40, 58), (60, 58), (60, 76), (80, 76), (80, 94), (100, 94), (100, 104)]
    d.polygon(pts, fill=conc, outline=OUT_C)
    d.line([(36, 24), (104, 78)], fill=steel, width=6)
    for x, y in ((50, 34), (72, 52), (94, 70)):
        d.line([(x, y), (x, y + 22)], fill=(196, 40, 36, 255), width=4)

def manual(d, c):
    d.rectangle([12, 72, 116, 104], fill=conc, outline=OUT_C)
    d.line([(12, 72), (116, 72)], fill=steel, width=4)

def ledge(d, c):
    d.rectangle([20, 56, 108, 104], fill=conc, outline=OUT_C)
    d.line([(20, 56), (108, 56)], fill=steel, width=5)

for n, f in (('quarter_pipe', qp), ('half_pipe', hp), ('kicker', kicker), ('funbox', funbox), ('bank', bank), ('rail', rail),
             ('stair_set', stairs), ('manual_pad', manual), ('ledge', ledge)):
    icon(n, f, None)

# ---------------- models and blockstates
jw(f'{A}/models/block/skatepark_concrete.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': 'freerun:block/skatepark_concrete'}})
jw(f'{A}/blockstates/skatepark_concrete.json', {'variants': {'': {'model': 'freerun:block/skatepark_concrete'}}})
jw(f'{A}/models/item/skatepark_concrete.json', {'parent': 'freerun:block/skatepark_concrete'})

def faces(tex, uvs=None):
    return {f: {'texture': tex} for f in ('north', 'south', 'east', 'west', 'up', 'down')}

post = {'from': [7, 0, 7], 'to': [9, 9, 9], 'faces': faces('#post')}
bar = {'from': [7, 9, 0], 'to': [9, 11, 16], 'faces': faces('#bar')}
jw(f'{A}/models/block/grind_rail_flat.json', {'parent': 'minecraft:block/block', 'textures': {'bar': 'freerun:block/grind_rail',
    'post': 'freerun:block/grind_rail_post', 'particle': 'freerun:block/grind_rail'}, 'elements': [post, bar]})
for name, ang in (('grind_rail_up', -45), ('grind_rail_down', 45)):
    jw(f'{A}/models/block/{name}.json', {'parent': 'minecraft:block/block', 'textures': {'bar': 'freerun:block/grind_rail',
        'post': 'freerun:block/grind_rail_post', 'particle': 'freerun:block/grind_rail'}, 'elements': [
        {'from': [7, 0, 7], 'to': [9, 13.4, 9], 'faces': faces('#post')},
        {'from': [7, 12.4, 0], 'to': [9, 14.4, 16], 'rotation': {'origin': [8, 13.4, 8], 'axis': 'x', 'angle': ang, 'rescale': True},
         'faces': faces('#bar')}]})
var = {}
for ax in ('x', 'z'):
    rot = 90 if ax == 'x' else 0
    var[f'axis={ax},slope=0'] = {'model': 'freerun:block/grind_rail_flat', 'y': rot}
    up, down = ('freerun:block/grind_rail_up', 'freerun:block/grind_rail_down')
    if ax == 'z':
        var['axis=z,slope=1'] = {'model': up}
        var['axis=z,slope=2'] = {'model': down}
    else:
        var['axis=x,slope=1'] = {'model': down, 'y': 90}
        var['axis=x,slope=2'] = {'model': up, 'y': 90}
for k in var:
    if var[k].get('y') == 0:
        del var[k]['y']
jw(f'{A}/blockstates/grind_rail.json', {'variants': var})
jw(f'{A}/models/item/grind_rail.json', {'parent': 'freerun:block/grind_rail_flat', 'display': {
    'gui': {'rotation': [30, 45, 0], 'translation': [0, 0, 0], 'scale': [0.9, 0.9, 0.9]}}})

sign_plate = {'from': [2, 3, 7.4], 'to': [14, 16, 8.6], 'faces': {
    'north': {'texture': '#f', 'uv': [0, 0, 16, 16]}, 'south': {'texture': '#b'},
    'east': {'texture': '#b'}, 'west': {'texture': '#b'}, 'up': {'texture': '#b'}, 'down': {'texture': '#b'}}}
jw(f'{A}/models/block/bus_stop_sign.json', {'parent': 'minecraft:block/block', 'textures': {'f': 'freerun:block/bus_stop_sign',
    'b': 'freerun:block/sign_back', 'p': 'freerun:block/pole', 'particle': 'freerun:block/bus_stop_sign'}, 'elements': [
    {'from': [7.5, 0, 7.5], 'to': [8.5, 16, 8.5], 'faces': faces('#p')}, sign_plate]})
jw(f'{A}/blockstates/bus_stop_sign.json', {'variants': {f'facing={f}': ({'model': 'freerun:block/bus_stop_sign', 'y': y} if y else {'model': 'freerun:block/bus_stop_sign'})
                                                        for f, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270))}})
jw(f'{A}/models/item/bus_stop_sign.json', {'parent': 'freerun:block/bus_stop_sign'})

kits = ['quarter_pipe', 'half_pipe', 'kicker', 'funbox', 'bank', 'rail', 'stair_set', 'manual_pad', 'ledge']
for k in kits:
    jw(f'{A}/models/item/kit_{k}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'freerun:item/kit_{k}'}})

# ---------------- loot
for b, drop in (('skatepark_concrete', 'skatepark_concrete'), ('grind_rail', 'grind_rail'), ('bus_stop_sign', 'bus_stop_sign'),
                ('skate_ramp', 'skatepark_concrete')):
    jw(f'{D}/loot_table/blocks/{b}.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'freerun:{drop}'}],
        'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})

# ---------------- recipes
def shaped(name, pattern, key, out, n=1):
    k = {}
    for c, v in key.items():
        k[c] = {'tag': v[1:]} if v.startswith('#') else {'item': v}
    jw(f'{D}/recipe/{name}.json', {'type': 'minecraft:crafting_shaped', 'category': 'building', 'pattern': pattern, 'key': k,
        'result': {'id': out, 'count': n}})
C, P, I, Rl = 'freerun:skatepark_concrete', '#minecraft:planks', 'minecraft:iron_ingot', 'freerun:grind_rail'
shaped('skatepark_concrete', ['SS', 'SS'], {'S': 'minecraft:smooth_stone'}, C, 4)
shaped('grind_rail', ['III', 'B B'], {'I': I, 'B': 'minecraft:iron_bars'}, Rl, 6)
shaped('kit_kicker', ['  P', ' PP', 'PPP'], {'P': P}, 'freerun:kit_kicker')
shaped('kit_bank', ['  C', ' CC', 'CCC'], {'C': C}, 'freerun:kit_bank')
shaped('kit_quarter_pipe', ['  P', ' PC', 'PCC'], {'P': P, 'C': C}, 'freerun:kit_quarter_pipe')
shaped('kit_half_pipe', ['P P', 'PCP', 'CCC'], {'P': P, 'C': C}, 'freerun:kit_half_pipe')
shaped('kit_funbox', [' R ', 'PCP'], {'R': Rl, 'P': P, 'C': C}, 'freerun:kit_funbox')
shaped('kit_rail', ['RRR'], {'R': Rl}, 'freerun:kit_rail')
shaped('kit_stair_set', ['R  ', 'CR ', 'CCR'], {'R': Rl, 'C': C}, 'freerun:kit_stair_set')
shaped('kit_manual_pad', ['CCC'], {'C': C}, 'freerun:kit_manual_pad')
shaped('kit_ledge', ['III', 'CCC'], {'I': 'minecraft:iron_nugget', 'C': C}, 'freerun:kit_ledge')

# ---------------- sounds: an air-brake hiss and a diesel pull-away
SR = 44100
def bp(x, lo, hi):
    b, a = butter(2, [lo / (SR / 2), hi / (SR / 2)], 'band'); return lfilter(b, a, x)
def lpf(x, hi):
    b, a = butter(2, hi / (SR / 2), 'low'); return lfilter(b, a, x)
def wsave(name, x):
    x = x / (np.max(np.abs(x)) + 1e-9) * 0.9
    wav = f'{OUT}/{name}.wav'
    with wave.open(wav, 'wb') as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes((x * 32767).astype(np.int16).tobytes())
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', wav, '-c:a', 'libvorbis', '-q:a', '5', f'{A}/sounds/{name}.ogg'], check=True)
t = np.arange(int(SR * 1.4)) / SR
hiss = bp(rng.standard_normal(len(t)), 2500, 9000) * np.minimum(1, t / 0.02) * np.exp(-t / 0.45)
clunk = np.sin(2 * np.pi * 70 * t) * np.exp(-t / 0.06)
wsave('bus_brake', hiss + 0.6 * clunk)
t = np.arange(int(SR * 2.2)) / SR
f0 = 38 + 30 * np.minimum(1, t / 1.6)
ph = np.cumsum(2 * np.pi * f0 / SR)
eng = np.sign(np.sin(ph)) * 0.5 + np.sin(2 * ph) * 0.3 + lpf(rng.standard_normal(len(t)), 300) * 0.6
eng = lpf(eng, 900) * np.minimum(1, t / 0.15) * np.minimum(1, (2.2 - t) / 0.4)
wsave('bus_engine', eng)
snd = json.load(open(f'{A}/sounds.json'))
for k, dist in (('bus_brake', 24), ('bus_engine', 32)):
    snd[k] = {'sounds': [{'name': f'freerun:{k}', 'attenuation_distance': dist}], 'subtitle': f'subtitles.freerun.{k}'}
json.dump(snd, open(f'{A}/sounds.json', 'w'), indent=2)

# ---------------- lang
lang = json.load(open(f'{A}/lang/en_us.json'))
titles = {'quarter_pipe': 'Quarter Pipe', 'half_pipe': 'Half Pipe', 'kicker': 'Kicker Ramp', 'funbox': 'Funbox', 'bank': 'Bank Ramp',
          'rail': 'Grind Rail', 'stair_set': 'Stair Set & Handrail', 'manual_pad': 'Manual Pad', 'ledge': 'Grind Ledge'}
for k, v in titles.items():
    lang[f'item.freerun.kit_{k}'] = f'Skate Kit: {v}'
lang.update({'block.freerun.skatepark_concrete': 'Skate Park Concrete', 'block.freerun.grind_rail': 'Grind Rail',
             'block.freerun.skate_ramp': 'Skate Ramp', 'block.freerun.bus_stop_sign': 'Bus Stop Sign', 'entity.freerun.bus': 'City Bus',
             'subtitles.freerun.bus_brake': 'Bus air brakes hiss', 'subtitles.freerun.bus_engine': 'Bus pulls away'})
json.dump(lang, open(f'{A}/lang/en_us.json', 'w'), indent=1, ensure_ascii=False)
print('ok')

"""Skateboard textures: 64x64 layout at 4x (256x256). Must match SkateboardModel.java's texOffs."""
from PIL import Image, ImageDraw, ImageFilter
import numpy as np, math, json, os
A = '/home/claude/freerun/src/main/resources/assets/freerun'
S = 4
os.makedirs(f'{A}/textures/entity/skateboard', exist_ok=True)
rng = np.random.default_rng(11)
def c(h, a=255): h = h.lstrip('#'); return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)

def faces(u, v, w, h, d):
    return {'up': (u + d, v, w, d), 'down': (u + d + w, v, w, d),
            'right': (u, v + d, d, h), 'front': (u + d, v + d, w, h), 'left': (u + d + w, v + d, d, h), 'back': (u + 2 * d + w, v + d, w, h)}

def rect(im, r):
    x, y, w, h = r
    return (x * S, y * S, (x + w) * S, (y + h) * S)

def grip(w, h):
    a = np.zeros((h, w, 4)); a[..., 3] = 255
    g = 20 + 26 * rng.random((h, w))
    a[..., :3] = g[..., None]
    sparkle = rng.random((h, w)) > 0.985
    a[..., :3] = np.where(sparkle[..., None], 90, a[..., :3])
    return Image.fromarray(a.astype(np.uint8))

def plies(w, h):
    """The deck's edge: seven plies of maple, one dyed."""
    im = Image.new('RGBA', (w, h), c('d8b47a')); d = ImageDraw.Draw(im)
    for i in range(h):
        if i % 3 == 1: d.line([(0, i), (w, i)], fill=c('b88a50'))
    d.line([(0, h // 2), (w, h // 2)], fill=c('3a7ac8'))
    return im

def graphic(design, w, h):
    """The bottom graphic, drawn once for the whole length (nose, middle, tail) then cut up."""
    im = Image.new('RGBA', (w, h), (0, 0, 0, 255)); d = ImageDraw.Draw(im)
    if design == 0:   # Bayou: moon over the swamp, a cypress
        for y in range(h):
            t = y / h; d.line([(0, y), (w, y)], fill=(int(20 + 30 * t), int(60 + 60 * t), int(70 + 40 * t), 255))
        d.ellipse([w * 0.2, h * 0.08, w * 0.8, h * 0.08 + w * 0.6], fill=c('f0e8c0'))
        d.rectangle([w * 0.45, h * 0.35, w * 0.55, h * 0.8], fill=c('1a2a20'))
        for k in range(5):
            y = h * (0.3 + k * 0.06); d.polygon([(w * 0.5, y - h * 0.05), (w * 0.1, y + h * 0.04), (w * 0.9, y + h * 0.04)], fill=c('1a3a24'))
        d.rectangle([0, h * 0.8, w, h], fill=c('0e2a2a'))
        for k in range(6): d.line([(w * 0.1, h * (0.83 + k * 0.025)), (w * 0.9, h * (0.83 + k * 0.025))], fill=c('2a6a6a'))
    elif design == 1:   # Gator: jaws open the length of the board
        d.rectangle([0, 0, w, h], fill=c('2a5a2a'))
        for y in range(0, h, 6):
            for x in range(0, w, 6): d.rectangle([x + 1, y + 1, x + 4, y + 4], fill=c('3a7a34'))
        d.polygon([(w * 0.5, h * 0.05), (w * 0.95, h * 0.35), (w * 0.95, h * 0.95), (w * 0.05, h * 0.95), (w * 0.05, h * 0.35)], fill=c('4a8a3a'))
        d.rectangle([w * 0.2, h * 0.35, w * 0.8, h * 0.85], fill=c('8a1a1a'))
        for k in range(10):
            y = h * (0.37 + k * 0.05)
            d.polygon([(w * 0.2, y), (w * 0.35, y + h * 0.02), (w * 0.2, y + h * 0.04)], fill=c('f0f0e0'))
            d.polygon([(w * 0.8, y), (w * 0.65, y + h * 0.02), (w * 0.8, y + h * 0.04)], fill=c('f0f0e0'))
        for x in (0.3, 0.7):
            d.ellipse([w * x - 8, h * 0.18, w * x + 8, h * 0.18 + 16], fill=c('f0d040')); d.ellipse([w * x - 2, h * 0.18 + 2, w * x + 2, h * 0.18 + 14], fill=c('101010'))
    elif design == 2:   # Bolt: a lightning bolt on purple
        for y in range(h):
            t = y / h; d.line([(0, y), (w, y)], fill=(int(70 + 60 * t), int(20 + 10 * t), int(120 + 40 * t), 255))
        d.polygon([(w * 0.6, h * 0.04), (w * 0.15, h * 0.52), (w * 0.48, h * 0.5), (w * 0.3, h * 0.96), (w * 0.88, h * 0.42), (w * 0.52, h * 0.44), (w * 0.78, h * 0.04)], fill=c('f8e040'))
        d.line([(w * 0.6, h * 0.06), (w * 0.2, h * 0.5)], fill=c('fff8c0'), width=3)
    else:   # Crawfish: a red mudbug on cream with a stripe
        d.rectangle([0, 0, w, h], fill=c('f0e6cc'))
        d.rectangle([0, h * 0.0, w, h * 0.06], fill=c('c03020')); d.rectangle([0, h * 0.94, w, h], fill=c('c03020'))
        cx = w / 2
        d.ellipse([cx - w * 0.22, h * 0.3, cx + w * 0.22, h * 0.7], fill=c('c03a20'))
        for k in range(4): d.line([(cx - w * 0.22, h * (0.38 + k * 0.08)), (cx + w * 0.22, h * (0.38 + k * 0.08))], fill=c('8a2010'), width=2)
        d.polygon([(cx, h * 0.7), (cx - w * 0.25, h * 0.88), (cx + w * 0.25, h * 0.88)], fill=c('c03a20'))
        for sx in (-1, 1):
            d.ellipse([cx + sx * w * 0.3 - 14, h * 0.1, cx + sx * w * 0.3 + 14, h * 0.28], fill=c('d84a28'))
            d.line([(cx + sx * w * 0.1, h * 0.33), (cx + sx * w * 0.28, h * 0.25)], fill=c('8a2010'), width=4)
            d.line([(cx + sx * w * 0.08, h * 0.3), (cx + sx * w * 0.35, h * 0.05)], fill=c('3a1a10'), width=2)
    return im

DESIGNS = ['bayou', 'gator', 'bolt', 'crawfish']
WHEELS = [c('f0e8d0'), c('40c8e8'), c('f04080'), c('f8d040')]

def paint(design):
    tex = Image.new('RGBA', (64 * S, 64 * S), (0, 0, 0, 0))
    # deck middle (0,0) 7 x 1 x 18 ; nose (0,20) 7 x 1 x 5 ; tail (24,20) 7 x 1 x 5
    parts = [((0, 0), 7, 1, 18), ((0, 20), 7, 1, 5), ((24, 20), 7, 1, 5)]
    # the graphic runs nose → middle → tail: 5 + 18 + 5 = 28 long
    G = graphic(design, 7 * S, 28 * S)
    offs = [5, 0, 23]   # where each piece starts along the graphic
    for (uv, w, h, d), off in zip(parts, offs):
        f = faces(uv[0], uv[1], w, h, d)
        top = grip(w * S, d * S)
        # nose/tail: the grip tape's edge is filed, a lighter rim
        dd = ImageDraw.Draw(top); dd.rectangle([0, 0, w * S - 1, d * S - 1], outline=c('5a5a5a'))
        tex.paste(top, rect(tex, f['up'])[:2])
        piece = G.crop((0, off * S, w * S, (off + d) * S))
        if uv == (0, 20): piece = G.crop((0, 0, w * S, 5 * S))
        if uv == (24, 20): piece = G.crop((0, 23 * S, w * S, 28 * S))
        tex.paste(piece, rect(tex, f['down'])[:2])
        for side in ('right', 'left'):
            tex.paste(plies(d * S, h * S), rect(tex, f[side])[:2])
        for side in ('front', 'back'):
            tex.paste(plies(w * S, h * S), rect(tex, f[side])[:2])
    # trucks: baseplate (0,27) 4x1x3, hanger (14,27) 9x1x1 -> brushed aluminium; wheels (34,27) 2x3x3
    def metal(w, h, shade=1.0):
        a = np.zeros((h, w, 4)); a[..., 3] = 255
        base = np.array([175, 180, 188]) * shade
        a[..., :3] = base * (0.85 + 0.15 * np.sin(np.arange(w)[None, :] * 0.9 + rng.random((h, w))))[..., None]
        return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8))
    for uv, w, h, d, sh in [((0, 27), 4, 1, 3, 0.8), ((14, 27), 9, 1, 1, 1.0)]:
        for k, r in faces(uv[0], uv[1], w, h, d).items():
            rr = rect(tex, r); tex.paste(metal(rr[2] - rr[0], rr[3] - rr[1], sh), rr[:2])
    wc = WHEELS[DESIGNS.index(design)]
    for k, r in faces(34, 27, 2, 3, 3).items():
        rr = rect(tex, r); W_, H_ = rr[2] - rr[0], rr[3] - rr[1]
        im = Image.new('RGBA', (W_, H_), wc); d = ImageDraw.Draw(im)
        if k in ('right', 'left'):   # the wheel's face: a bearing in the middle
            d.ellipse([W_ * 0.3, H_ * 0.3, W_ * 0.7, H_ * 0.7], fill=c('6a6a70')); d.ellipse([W_ * 0.42, H_ * 0.42, W_ * 0.58, H_ * 0.58], fill=c('2a2a2a'))
        else:
            for y in range(0, H_, 3): d.line([(0, y), (W_, y)], fill=tuple(int(v * 0.9) for v in wc[:3]) + (255,))
        tex.paste(im, rr[:2])
    tex.save(f'{A}/textures/entity/skateboard/{design}.png')
    return tex

for dsn in DESIGNS: paint(dsn)
# item icons (for the inventory's flat fallbacks: the 3D model is drawn by the item renderer)
prev = Image.new('RGBA', (4 * 120, 300), (38, 40, 46, 255))
for i, dsn in enumerate(DESIGNS):
    G = graphic(i, 7 * S, 28 * S).resize((7 * S * 2, 28 * S * 2), Image.NEAREST)
    prev.paste(G, (i * 120 + 32, 20))
prev.save('/tmp/claude-0/-home-claude/3a456fdc-7b05-56c9-bf69-bc83c00ecb1d/scratchpad/decks.png')
print('ok')

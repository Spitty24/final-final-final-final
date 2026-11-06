"""
The Swampwatch roster: ten original bayou creatures. One spec per creature drives
  - the Java LayerDefinition (AlienRigs.java),
  - a 4x-resolution skin painted per box face (textures/entity/alien/<id>.png),
  - a 64x64 dial icon.
Model space: 16 units = 1 block, y grows DOWN, ground is y = 24.
"""
from PIL import Image, ImageDraw, ImageFilter
import numpy as np, json, math, random, os

R = '/home/claude/freerun/src/main/resources/assets/freerun'
OUT_JAVA = '/home/claude/freerun/src/main/java/dev/freerun/client/alien/AlienRigs.java'
SCALE = 4   # texture pixels per model unit

def hx(h):
    h = h.lstrip('#'); return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16))

# ---------------------------------------------------------------- specs
# part: (name, pivot(x,y,z), [box, ...], colour key)
# box: (x, y, z, w, h, d) relative to the pivot, optional 7th = inflate
def P(name, pivot, boxes, colour='base'):
    return dict(name=name, pivot=pivot, boxes=boxes, colour=colour)

ALIENS = [
 dict(id='mossback', name='Mossback', material='shell', w=1.3, h=1.7, eye=1.45,
      colours=dict(base='4f7a3c', dark='2f4a24', shell='5a4a2a', shell2='8a6a36', belly='b8a56a', eye='e8d44a'),
      parts=[
        P('body', (0, 11, 0), [(-8, -5, -5, 16, 11, 10)], 'base'),
        P('belly', (0, 11, 0), [(-6, -4, -6, 12, 9, 1)], 'belly'),
        P('shell', (0, 8, 5), [(-10, -3, 0, 20, 15, 7), (-8, -5, 1, 16, 2, 5), (-9, 12, 1, 18, 2, 5)], 'shell'),
        P('head', (0, 7, -4), [(-4, -6, -6, 8, 7, 8)], 'base'),
        P('armL', (9, 7, 0), [(-2.5, -1, -2.5, 5, 13, 5)], 'base'),
        P('armR', (-9, 7, 0), [(-2.5, -1, -2.5, 5, 13, 5)], 'base'),
        P('legL', (4, 17, 0), [(-3, 0, -3, 6, 7, 6)], 'dark'),
        P('legR', (-4, 17, 0), [(-3, 0, -3, 6, 7, 6)], 'dark'),
        P('tail', (0, 16, 5), [(-1.5, -1, 0, 3, 3, 6)], 'dark'),
      ]),
 dict(id='skeeter', name='Skeeter', material='chitin', w=0.6, h=1.25, eye=1.05,
      colours=dict(base='3a3a44', dark='1c1c22', stripe='c8c0a0', wing='b8d8e8', eye='c02020'),
      parts=[
        P('body', (0, 12, 0), [(-2, -4, -2, 4, 9, 4), (-2.5, -6, -2.5, 5, 3, 5)], 'base'),
        P('abdomen', (0, 17, 1), [(-2, 0, -1, 4, 7, 3)], 'stripe'),
        P('head', (0, 6, -1), [(-2.5, -4, -3, 5, 5, 5), (-0.5, -1, -9, 1, 1, 6)], 'dark'),
        P('wingL', (1, 7, 2), [(0, 0, -2, 11, 1, 5)], 'wing'),
        P('wingR', (-1, 7, 2), [(-11, 0, -2, 11, 1, 5)], 'wing'),
        P('armL', (2.5, 9, -1), [(-0.5, 0, -0.5, 1, 8, 1)], 'dark'),
        P('armR', (-2.5, 9, -1), [(-0.5, 0, -0.5, 1, 8, 1)], 'dark'),
        P('legL', (1.5, 16, 0), [(-0.5, 0, -0.5, 1, 8, 1)], 'dark'),
        P('legR', (-1.5, 16, 0), [(-0.5, 0, -0.5, 1, 8, 1)], 'dark'),
        P('legL2', (2, 14, 1), [(-0.5, 0, -0.5, 1, 10, 1)], 'dark'),
        P('legR2', (-2, 14, 1), [(-0.5, 0, -0.5, 1, 10, 1)], 'dark'),
      ]),
 dict(id='mudbug', name='Mudbug', material='chitin', w=1.0, h=1.9, eye=1.6,
      colours=dict(base='a0321e', dark='6a1e12', belly='d88a5a', eye='101010', claw='c04028'),
      parts=[
        P('body', (0, 10, 0), [(-5, -6, -4, 10, 12, 8), (-5.5, -6, -5, 11, 2, 1)], 'base'),
        P('belly', (0, 10, 0), [(-4, -4, -5, 8, 10, 1)], 'belly'),
        P('head', (0, 4, -2), [(-4, -5, -5, 8, 5, 7), (3, -9, -3, 1, 5, 1), (-4, -9, -3, 1, 5, 1), (2.5, -10, -3.5, 2, 2, 2), (-4.5, -10, -3.5, 2, 2, 2)], 'base'),
        P('antL', (2, 1, -5), [(-0.5, -1, -10, 1, 1, 10)], 'dark'),
        P('antR', (-2, 1, -5), [(-0.5, -1, -10, 1, 1, 10)], 'dark'),
        P('armL', (6, 6, 0), [(-1.5, -1, -1.5, 3, 9, 3)], 'base'),
        P('armR', (-6, 6, 0), [(-1.5, -1, -1.5, 3, 9, 3)], 'base'),
        P('pincerL', (6, 14, 0), [(-2.5, 0, -8, 5, 3, 9), (-2.5, 3, -8, 5, 2, 7)], 'claw'),
        P('pincerR', (-6, 14, 0), [(-2.5, 0, -8, 5, 3, 9), (-2.5, 3, -8, 5, 2, 7)], 'claw'),
        P('legL', (3, 16, 0), [(-1.5, 0, -1.5, 3, 8, 3)], 'dark'),
        P('legR', (-3, 16, 0), [(-1.5, 0, -1.5, 3, 8, 3)], 'dark'),
        P('sideL', (5.5, 12, 2), [(0, 0, -0.5, 5, 1, 1), (0, 0, 2.5, 5, 1, 1)], 'dark'),
        P('sideR', (-5.5, 12, 2), [(-5, 0, -0.5, 5, 1, 1), (-5, 0, 2.5, 5, 1, 1)], 'dark'),
        P('tail', (0, 15, 4), [(-3, 0, 0, 6, 4, 6), (-5, 3, 5, 10, 1, 5)], 'base'),
      ]),
 dict(id='voltbelly', name='Voltbelly', material='electric', w=0.7, h=2.2, eye=1.95,
      colours=dict(base='1e3a6e', dark='0e1e40', belly='e8e060', fin='78a8e8', eye='f0f080'),
      parts=[
        P('head', (0, -4, 0), [(-3, -6, -6, 6, 6, 10), (-3.5, -3, -7, 7, 2, 2)], 'base'),
        P('crest', (0, -9, 0), [(-0.5, -3, -5, 1, 3, 12)], 'fin'),
        P('body', (0, 4, 0), [(-3, -2, -3, 6, 14, 6)], 'base'),
        P('belly', (0, 4, 0), [(-2, -1, -4, 4, 12, 1)], 'belly'),
        P('armL', (3.5, 0, 0), [(-0.5, 0, -2, 1, 9, 4)], 'fin'),
        P('armR', (-3.5, 0, 0), [(-0.5, 0, -2, 1, 9, 4)], 'fin'),
        P('tail1', (0, 16, 0), [(-3.5, 0, -3.5, 7, 5, 7)], 'base'),
        P('tail2', (0, 20, 2), [(-5, 0, -6, 10, 4, 12)], 'dark'),
        P('finBack', (0, 2, 3), [(-0.5, 0, 0, 1, 14, 3)], 'fin'),
      ]),
 dict(id='cypress', name='Cypress', material='bark', w=0.9, h=3.0, eye=2.7,
      colours=dict(base='6a4a2e', dark='3e2a18', leaf='4a7a30', leaf2='7aa040', moss='8aa050', eye='e8c850'),
      parts=[
        P('body', (0, 0, 0), [(-4, -8, -4, 8, 22, 8), (-5, 4, -5, 10, 4, 10)], 'base'),
        P('head', (0, -8, 0), [(-5, -8, -5, 10, 8, 10)], 'base'),
        P('canopy', (0, -16, 0), [(-9, -3, -9, 18, 4, 18), (-6, -6, -6, 12, 3, 12), (-3, -8, -3, 6, 2, 6)], 'leaf'),
        P('armL', (5, -4, 0), [(-1.5, -1, -1.5, 3, 16, 3), (0, 10, -1, 6, 2, 2)], 'base'),
        P('armR', (-5, -4, 0), [(-1.5, -1, -1.5, 3, 16, 3), (-6, 10, -1, 6, 2, 2)], 'base'),
        P('legL', (3, 14, 0), [(-2.5, 0, -2.5, 5, 10, 5), (-3.5, 8, -3.5, 7, 2, 7)], 'dark'),
        P('legR', (-3, 14, 0), [(-2.5, 0, -2.5, 5, 10, 5), (-3.5, 8, -3.5, 7, 2, 7)], 'dark'),
        P('kneeL', (7, 20, -3), [(-1, 0, -1, 2, 4, 2)], 'dark'),
        P('kneeR', (-7, 21, 2), [(-1, 0, -1, 2, 3, 2)], 'dark'),
        P('mossL', (-5, 2, -4), [(0, 0, 0, 3, 6, 1)], 'moss'),
      ]),
 dict(id='foglet', name='Foglet', material='mist', w=0.7, h=1.6, eye=1.4, alpha=0.6,
      colours=dict(base='c8d8e8', dark='9ab0c8', glow='e8f4ff', eye='60e0ff'),
      parts=[
        P('body', (0, 10, 0), [(-5, -5, -4, 10, 10, 8), (-6, -2, -2, 12, 5, 5)], 'base'),
        P('head', (0, 4, 0), [(-4, -7, -4, 8, 7, 8), (-3, -9, -3, 6, 3, 6)], 'glow'),
        P('armL', (6, 7, 0), [(-1.5, 0, -1.5, 3, 10, 3), (-2, 8, -2, 4, 3, 4)], 'dark'),
        P('armR', (-6, 7, 0), [(-1.5, 0, -1.5, 3, 10, 3), (-2, 8, -2, 4, 3, 4)], 'dark'),
        P('tail', (0, 15, 0), [(-4, 0, -3, 8, 6, 6), (-2.5, 5, -2, 5, 4, 4)], 'dark'),
      ]),
 dict(id='clockroach', name='Clockroach', material='chitin', w=0.45, h=0.5, eye=0.4,
      colours=dict(base='6a3a1a', dark='3a2010', shell='8a5020', eye='f0e060'),
      parts=[
        P('body', (0, 20, 0), [(-3, -1, -4, 6, 3, 9)], 'base'),
        P('shell', (0, 19, 0), [(-3.5, -1, -3, 7, 1, 8)], 'shell'),
        P('head', (0, 20, -4), [(-2, -1, -4, 4, 3, 4)], 'dark'),
        P('antL', (1, 19, -8), [(-0.25, -8, -0.25, 1, 8, 1)], 'dark'),
        P('antR', (-1, 19, -8), [(-0.25, -8, -0.25, 1, 8, 1)], 'dark'),
        P('armL', (3, 21, -2), [(0, 0, -0.5, 4, 1, 1), (3, 0, -0.5, 1, 3, 1)], 'dark'),
        P('armR', (-3, 21, -2), [(-4, 0, -0.5, 4, 1, 1), (-4, 0, -0.5, 1, 3, 1)], 'dark'),
        P('legL', (3, 21, 1), [(0, 0, -0.5, 4, 1, 1), (3, 0, -0.5, 1, 3, 1)], 'dark'),
        P('legR', (-3, 21, 1), [(-4, 0, -0.5, 4, 1, 1), (-4, 0, -0.5, 1, 3, 1)], 'dark'),
        P('legL2', (3, 21, 4), [(0, 0, -0.5, 4, 1, 1), (3, 0, -0.5, 1, 3, 1)], 'dark'),
        P('legR2', (-3, 21, 4), [(-4, 0, -0.5, 4, 1, 1), (-4, 0, -0.5, 1, 3, 1)], 'dark'),
      ]),
 dict(id='bayou_bull', name='Bayou Bull', material='fur', w=1.4, h=2.3, eye=2.0,
      colours=dict(base='4a2e1e', dark='2a1a10', belly='7a5a40', horn='e8dcc0', ring='c8a030', eye='e03020'),
      parts=[
        P('body', (0, 0, 0), [(-9, -2, -5, 18, 14, 10)], 'base'),
        P('belly', (0, 0, 0), [(-7, 2, -6, 14, 9, 1)], 'belly'),
        P('head', (0, -2, -4), [(-5, -9, -7, 10, 9, 9), (-3, -3, -10, 6, 4, 3)], 'dark'),
        P('hornL', (5, -9, -4), [(0, -1, -1, 7, 2, 2), (6, -5, -1, 2, 5, 2)], 'horn'),
        P('hornR', (-5, -9, -4), [(-7, -1, -1, 7, 2, 2), (-8, -5, -1, 2, 5, 2)], 'horn'),
        P('ring', (0, -1, -10), [(-1.5, 0, -1, 3, 3, 1)], 'ring'),
        P('armL', (11, 0, 0), [(-3.5, -1, -3.5, 7, 14, 7)], 'base'),
        P('armR', (-11, 0, 0), [(-3.5, -1, -3.5, 7, 14, 7)], 'base'),
        P('legL', (5, 12, 0), [(-3, 0, -3, 6, 12, 6), (-3.5, 9, -3.5, 7, 3, 7)], 'dark'),
        P('legR', (-5, 12, 0), [(-3, 0, -3, 6, 12, 6), (-3.5, 9, -3.5, 7, 3, 7)], 'dark'),
        P('tail', (0, 2, 5), [(-1, 0, 0, 2, 12, 2)], 'dark'),
      ]),
 dict(id='slagworm', name='Slagworm', material='slag', w=1.0, h=2.0, eye=1.75,
      colours=dict(base='2a2426', dark='181416', glow='ff8a20', glow2='ffd040', eye='ffe080'),
      parts=[
        P('tail', (0, 24, 0), [(-6, -6, -6, 12, 6, 12)], 'dark'),
        P('tail1', (0, 18, 0), [(-5, -6, -5, 10, 6, 10)], 'base'),
        P('body', (0, 12, 0), [(-4.5, -8, -4.5, 9, 8, 9)], 'base'),
        P('head', (0, 4, 0), [(-5, -8, -6, 10, 8, 11), (-4.5, -2, -9, 9, 2, 4)], 'base'),
        P('jaw', (0, 1, -4), [(-4, 0, -5, 8, 2, 6)], 'dark'),
        P('armL', (5, 6, 0), [(-1, 0, -1, 2, 6, 2)], 'dark'),
        P('armR', (-5, 6, 0), [(-1, 0, -1, 2, 6, 2)], 'dark'),
      ]),
 dict(id='gumbo', name='Gumbo', material='slime', w=0.9, h=1.4, eye=1.1, alpha=0.85,
      colours=dict(base='7a4a1e', dark='5a3010', bit='c84a2a', bit2='3a7a30', eye='f0f0e0'),
      parts=[
        P('body', (0, 12, 0), [(-6, -5, -5, 12, 11, 10, 1)], 'base'),
        P('bits', (0, 12, 0), [(-5, -3, -6, 2, 2, 1), (2, 1, -6, 3, 2, 1), (-7, 2, -2, 1, 3, 2), (6, -2, 0, 1, 2, 2)], 'bit'),
        P('head', (0, 7, 0), [(-4, -6, -4, 8, 6, 8, 0.5)], 'base'),
        P('armL', (7, 10, 0), [(-1.5, 0, -1.5, 3, 8, 3), (-1, 7, -1, 2, 3, 2)], 'dark'),
        P('armR', (-7, 10, 0), [(-1.5, 0, -1.5, 3, 8, 3), (-1, 7, -1, 2, 3, 2)], 'dark'),
        P('legL', (3, 18, 0), [(-2.5, 0, -2.5, 5, 6, 5)], 'dark'),
        P('legR', (-3, 18, 0), [(-2.5, 0, -2.5, 5, 6, 5)], 'dark'),
        P('drip', (-4, 17, -3), [(-1, 0, -1, 2, 5, 2)], 'base'),
      ]),
]

# ---------------------------------------------------------------- UV packing
def pack(spec):
    """Assign texOffs to every box; returns layout (w, h) in model units."""
    items = []
    for part in spec['parts']:
        for b in part['boxes']:
            w, h, d = b[3], b[4], b[5]
            W, H = int(math.ceil(2 * (w + d))), int(math.ceil(h + d))
            items.append((part, b, W, H))
    items.sort(key=lambda it: -it[3])
    width = 128
    while True:
        x = y = rowH = 0; placed = []; ok = True
        for part, b, W, H in items:
            if x + W > width:
                x = 0; y += rowH; rowH = 0
            placed.append((part, b, x, y)); x += W; rowH = max(rowH, H)
        total = y + rowH
        if total <= width: break
        width *= 2
    layout = {}
    for part, b, u, v in placed:
        layout.setdefault(id(part), []).append((u, v))
    return width, layout

# ---------------------------------------------------------------- Java
JAVA_HEAD = '''package dev.freerun.client.alien;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** The ten Swampwatch creatures' rigs. GENERATED by gen/aliens.py: edit the spec there, not here. */
public final class AlienRigs {
    private AlienRigs() {}

    public static LayerDefinition rig(dev.freerun.alien.Alien alien) {
        return switch (alien) {
%s
        };
    }
%s
}
'''

def java_for(spec, width, layout):
    lines = ['    private static LayerDefinition %s() {' % spec['id'],
             '        MeshDefinition mesh = new MeshDefinition();',
             '        PartDefinition root = mesh.getRoot();']
    for part in spec['parts']:
        offs = layout[id(part)]
        cl = ['CubeListBuilder.create()']
        for (u, v), b in zip(offs, part['boxes']):
            infl = b[6] if len(b) > 6 else 0
            cube = '.texOffs(%d, %d).addBox(%sf, %sf, %sf, %sf, %sf, %sf%s)' % (
                u, v, b[0], b[1], b[2], b[3], b[4], b[5], ', new CubeDeformation(%sf)' % infl if infl else '')
            cl.append(cube)
        px, py, pz = part['pivot']
        lines.append('        root.addOrReplaceChild("%s", %s,\n                PartPose.offset(%sf, %sf, %sf));' % (
            part['name'], '\n                '.join(cl), px, py, pz))
    lines.append('        return LayerDefinition.create(mesh, %d, %d);' % (width, width))
    lines.append('    }')
    return '\n'.join(lines)

# ---------------------------------------------------------------- painting
def noise(w, h, seed, octaves=4, persistence=0.55):
    rng = np.random.default_rng(seed)
    out = np.zeros((h, w)); amp = 1.0; total = 0
    for o in range(octaves):
        n = max(2, 2 ** (o + 2))
        g = rng.random((n + 1, n + 1))
        ys = np.linspace(0, n, h, endpoint=False); xs = np.linspace(0, n, w, endpoint=False)
        yi = ys.astype(int); xi = xs.astype(int); yf = ys - yi; xf = xs - xi
        yf = yf * yf * (3 - 2 * yf); xf = xf * xf * (3 - 2 * xf)
        a = g[yi][:, xi]; b = g[yi][:, xi + 1]; c = g[yi + 1][:, xi]; d = g[yi + 1][:, xi + 1]
        v = (a * (1 - xf) + b * xf) * (1 - yf)[:, None] + (c * (1 - xf) + d * xf) * yf[:, None]
        out += v * amp; total += amp; amp *= persistence
    return out / total

def shade(col, f):
    return tuple(int(max(0, min(255, c * f))) for c in col)

def material(mat, col, w, h, seed, spec):
    """Paint a w x h face. Returns RGBA numpy array."""
    rng = np.random.default_rng(seed)
    base = np.array(col, dtype=float)
    n = noise(w, h, seed)
    img = np.zeros((h, w, 4), dtype=float)
    yy, xx = np.mgrid[0:h, 0:w]
    if mat == 'shell':
        # mossy, lumpy skin with darker creases
        f = 0.75 + 0.5 * n
        crease = (np.sin(xx * 0.9 + n * 6) * np.sin(yy * 0.7 + n * 5)) > 0.85
        f = np.where(crease, f * 0.7, f)
        img[..., :3] = base * f[..., None]
    elif mat == 'chitin':
        # glossy plates in bands with a highlight ridge
        band = ((yy // 10) % 2 == 0)
        f = 0.7 + 0.4 * n + np.where(band, 0.12, -0.05)
        ridge = (yy % 10) < 2
        f = np.where(ridge, f * 1.35, f)
        img[..., :3] = base * f[..., None]
    elif mat == 'electric':
        f = 0.6 + 0.5 * n
        streak = np.abs(np.sin(yy * 0.35 + xx * 0.08 + n * 8)) > 0.97
        img[..., :3] = base * f[..., None]
        img[..., :3] = np.where(streak[..., None], np.array(hx(spec['colours']['eye'])) * 1.0, img[..., :3])
    elif mat == 'bark':
        ridges = np.abs(np.sin(xx * 0.5 + n * 9)) ** 3
        f = 0.55 + 0.6 * ridges + 0.2 * n
        img[..., :3] = base * f[..., None]
    elif mat == 'mist':
        f = 0.85 + 0.3 * n
        img[..., :3] = base * f[..., None]
        img[..., 3] = 255 * (0.55 + 0.45 * n)
        img[..., 3] = np.clip(img[..., 3] * (0.7 + 0.6 * (1 - np.abs(yy / max(1, h - 1) - 0.5) * 2)), 0, 255)
        return np.clip(img, 0, 255).astype(np.uint8)
    elif mat == 'fur':
        strands = (np.sin(yy * 1.2 + xx * 0.3 + n * 10) > 0.5)
        f = 0.7 + 0.35 * n + np.where(strands, 0.15, -0.1) * rng.random((h, w))
        img[..., :3] = base * f[..., None]
    elif mat == 'slag':
        # cooled crust with glowing cracks
        cracks = np.abs(n - 0.5) < 0.025
        hot = np.abs(n - 0.5) < 0.012
        f = 0.6 + 0.5 * noise(w, h, seed + 7, 3)
        img[..., :3] = base * f[..., None]
        g1 = np.array(hx(spec['colours']['glow']), dtype=float); g2 = np.array(hx(spec['colours']['glow2']), dtype=float)
        img[..., :3] = np.where(cracks[..., None], g1, img[..., :3])
        img[..., :3] = np.where(hot[..., None], g2, img[..., :3])
    elif mat == 'slime':
        f = 0.75 + 0.5 * n
        bubbles = ((xx - w * 0.3) ** 2 + (yy - h * 0.35) ** 2 < (min(w, h) * 0.12) ** 2) | ((xx - w * 0.7) ** 2 + (yy - h * 0.7) ** 2 < (min(w, h) * 0.08) ** 2)
        f = np.where(bubbles, f * 1.3, f)
        img[..., :3] = base * f[..., None]
        img[..., 3] = 255 * spec.get('alpha', 1.0)
        return np.clip(img, 0, 255).astype(np.uint8)
    else:
        img[..., :3] = base * (0.75 + 0.5 * n)[..., None]
    img[..., 3] = 255 * spec.get('alpha', 1.0)
    # a soft top-lit gradient on every face
    img[..., :3] *= (1.08 - 0.16 * (yy / max(1, h - 1)))[..., None]
    return np.clip(img, 0, 255).astype(np.uint8)

def faces(u, v, w, h, d):
    """Face rects in layout units: name -> (x, y, w, h), MC box layout."""
    return {'up': (u + d, v, w, d), 'down': (u + d + w, v, w, d),
            'right': (u, v + d, d, h), 'front': (u + d, v + d, w, h), 'left': (u + d + w, v + d, d, h), 'back': (u + 2 * d + w, v + d, w, h)}

def paint(spec, width, layout):
    S = SCALE
    tex = np.zeros((width * S, width * S, 4), dtype=np.uint8)
    seed = 0
    for part in spec['parts']:
        col = hx(spec['colours'][part['colour']])
        mat = spec['material']
        if part['colour'] in ('eye',): mat = 'plain'
        if part['colour'] in ('belly', 'wing', 'horn', 'ring', 'leaf', 'moss', 'bit', 'claw', 'fin', 'crest', 'stripe'):
            mat = {'wing': 'mist', 'leaf': 'fur', 'moss': 'fur', 'belly': 'plain', 'horn': 'bark', 'ring': 'plain', 'bit': 'plain',
                   'claw': 'chitin', 'fin': 'mist', 'stripe': 'chitin'}.get(part['colour'], 'plain')
        for (u, v), b in zip(layout[id(part)], part['boxes']):
            w, h, d = int(math.ceil(b[3])), int(math.ceil(b[4])), int(math.ceil(b[5]))
            for name, (fx, fy, fw, fh) in faces(u, v, w, h, d).items():
                seed += 1
                fw, fh = max(1, fw), max(1, fh)
                face = material(mat, col, fw * S, fh * S, seed, spec)
                if name == 'down': face = (face.astype(float) * [0.6, 0.6, 0.6, 1]).astype(np.uint8)
                if name in ('right', 'left'): face = (face.astype(float) * [0.85, 0.85, 0.85, 1]).astype(np.uint8)
                if name == 'back': face = (face.astype(float) * [0.75, 0.75, 0.75, 1]).astype(np.uint8)
                tex[fy * S:fy * S + fh * S, fx * S:fx * S + fw * S] = face
            if part['name'] == 'head':
                eyes(spec, tex, u, v, w, h, d, b)
    im = Image.fromarray(tex, 'RGBA')
    return im

def eyes(spec, tex, u, v, w, h, d, b):
    """Two eyes and a mouth on the head box's front face."""
    S = SCALE
    fx, fy, fw, fh = faces(u, v, w, h, d)['front']
    face = Image.fromarray(tex[fy * S:(fy + fh) * S, fx * S:(fx + fw) * S].copy(), 'RGBA')
    dr = ImageDraw.Draw(face)
    W, H = face.size
    eye = hx(spec['colours']['eye'])
    aid = spec['id']
    r = max(3, W // 9)
    ey = int(H * 0.38)
    for ex in (int(W * 0.28), int(W * 0.72)):
        if aid == 'mudbug':
            continue   # eyes are on stalks
        if aid in ('skeeter', 'clockroach'):
            dr.ellipse([ex - r * 1.6, ey - r * 1.3, ex + r * 1.6, ey + r * 1.3], fill=eye)   # compound eyes
            for k in range(-2, 3):
                dr.line([(ex - r * 1.6, ey + k * 3), (ex + r * 1.6, ey + k * 3)], fill=shade(eye, 0.7))
            continue
        dr.ellipse([ex - r * 1.3, ey - r * 1.1, ex + r * 1.3, ey + r * 1.1], fill=(250, 250, 240, 255))
        dr.ellipse([ex - r * 0.8, ey - r * 0.8, ex + r * 0.8, ey + r * 0.8], fill=eye)
        if aid == 'bayou_bull':
            dr.ellipse([ex - r * 0.35, ey - r * 0.6, ex + r * 0.35, ey + r * 0.6], fill=(20, 10, 10, 255))
        else:
            dr.ellipse([ex - r * 0.4, ey - r * 0.4, ex + r * 0.4, ey + r * 0.4], fill=(15, 15, 20, 255))
        dr.ellipse([ex - r * 0.15, ey - r * 0.55, ex + r * 0.15, ey - r * 0.25], fill=(255, 255, 255, 255))
    my = int(H * 0.72)
    if aid in ('mossback', 'cypress', 'gumbo', 'foglet'):
        dr.arc([W * 0.3, my - 8, W * 0.7, my + 8], 0, 180, fill=(20, 15, 15, 255), width=max(2, S))
    elif aid in ('bayou_bull',):
        for nx in (int(W * 0.42), int(W * 0.58)):
            dr.ellipse([nx - 4, my - 3, nx + 4, my + 3], fill=(20, 10, 10, 255))
    elif aid == 'slagworm':
        for k in range(5):
            x = int(W * (0.2 + 0.15 * k))
            dr.polygon([(x - 4, my - 6), (x + 4, my - 6), (x, my + 6)], fill=hx(spec['colours']['glow2']))
    elif aid == 'voltbelly':
        dr.line([(W * 0.2, my), (W * 0.8, my)], fill=(10, 10, 30, 255), width=3)
    tex[fy * S:(fy + fh) * S, fx * S:(fx + fw) * S] = np.array(face)

# ---------------------------------------------------------------- icons
def icon(spec):
    im = Image.new('RGBA', (64, 64), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    c = hx(spec['colours']['base']); dk = shade(c, 0.6); e = hx(spec['colours']['eye'])
    aid = spec['id']
    if aid == 'mossback':
        d.ellipse([6, 18, 58, 58], fill=hx(spec['colours']['shell'])); d.chord([6, 18, 58, 58], 180, 360, fill=hx(spec['colours']['shell2']))
        for x in range(12, 54, 11): d.line([(x, 24), (x + 4, 52)], fill=dk, width=2)
        d.ellipse([22, 6, 42, 26], fill=c); d.ellipse([27, 11, 31, 15], fill=e); d.ellipse([33, 11, 37, 15], fill=e)
    elif aid == 'skeeter':
        d.ellipse([26, 26, 38, 58], fill=c); d.ellipse([27, 14, 37, 26], fill=dk)
        d.polygon([(32, 20), (6, 8), (10, 32)], fill=hx(spec['colours']['wing'])); d.polygon([(32, 20), (58, 8), (54, 32)], fill=hx(spec['colours']['wing']))
        d.line([(32, 14), (32, 2)], fill=dk, width=2); d.ellipse([28, 16, 31, 19], fill=e); d.ellipse([33, 16, 36, 19], fill=e)
    elif aid == 'mudbug':
        d.polygon([(32, 8), (44, 30), (40, 58), (24, 58), (20, 30)], fill=c)
        d.pieslice([2, 14, 30, 42], 200, 340, fill=hx(spec['colours']['claw'])); d.pieslice([34, 14, 62, 42], 200, 340, fill=hx(spec['colours']['claw']))
        d.line([(26, 12), (14, 2)], fill=dk, width=2); d.line([(38, 12), (50, 2)], fill=dk, width=2)
    elif aid == 'voltbelly':
        d.line([(10, 54), (26, 26), (40, 40), (54, 10)], fill=c, width=10)
        d.polygon([(30, 6), (22, 30), (34, 30), (26, 58), (46, 26), (34, 26), (42, 6)], fill=hx(spec['colours']['belly']))
    elif aid == 'cypress':
        d.rectangle([28, 30, 36, 60], fill=c); d.polygon([(32, 4), (8, 40), (56, 40)], fill=hx(spec['colours']['leaf'])); d.polygon([(32, 14), (14, 34), (50, 34)], fill=hx(spec['colours']['leaf2']))
        d.ellipse([26, 40, 30, 44], fill=e); d.ellipse([34, 40, 38, 44], fill=e)
    elif aid == 'foglet':
        for (x, y, r) in [(20, 36, 14), (34, 26, 16), (46, 38, 13), (32, 42, 15)]: d.ellipse([x - r, y - r, x + r, y + r], fill=c + (200,))
        d.ellipse([27, 28, 31, 32], fill=e); d.ellipse([37, 28, 41, 32], fill=e)
    elif aid == 'clockroach':
        d.ellipse([20, 14, 44, 56], fill=hx(spec['colours']['shell'])); d.ellipse([24, 6, 40, 20], fill=dk)
        for k in range(3):
            y = 22 + k * 11; d.line([(22, y), (6, y - 6)], fill=dk, width=3); d.line([(42, y), (58, y - 6)], fill=dk, width=3)
        d.line([(28, 8), (16, 0)], fill=dk, width=2); d.line([(36, 8), (48, 0)], fill=dk, width=2)
    elif aid == 'bayou_bull':
        d.ellipse([16, 14, 48, 54], fill=c); d.ellipse([22, 36, 42, 56], fill=hx(spec['colours']['belly']))
        d.arc([2, 2, 30, 34], 180, 300, fill=hx(spec['colours']['horn']), width=6); d.arc([34, 2, 62, 34], 240, 360, fill=hx(spec['colours']['horn']), width=6)
        d.ellipse([24, 24, 29, 29], fill=e); d.ellipse([35, 24, 40, 29], fill=e); d.ellipse([28, 50, 36, 58], fill=hx(spec['colours']['ring']))
    elif aid == 'slagworm':
        d.line([(10, 56), (20, 44), (34, 50), (44, 30), (50, 10)], fill=c, width=14)
        d.line([(12, 54), (22, 44), (34, 48), (44, 32), (50, 12)], fill=hx(spec['colours']['glow']), width=3)
        d.ellipse([42, 2, 58, 18], fill=c); d.ellipse([47, 7, 51, 11], fill=e)
    elif aid == 'gumbo':
        d.ellipse([6, 18, 58, 60], fill=c); d.ellipse([20, 6, 44, 30], fill=c)
        d.ellipse([16, 36, 22, 42], fill=hx(spec['colours']['bit'])); d.ellipse([40, 44, 48, 52], fill=hx(spec['colours']['bit2']))
        d.ellipse([26, 14, 30, 18], fill=e); d.ellipse([34, 14, 38, 18], fill=e)
    return im

# ---------------------------------------------------------------- the watch
def watch_icon():
    im = Image.new('RGBA', (64, 64), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    d.rounded_rectangle([22, 2, 42, 62], 6, fill=(40, 48, 52, 255))              # strap
    for y in range(6, 60, 6): d.line([(24, y), (40, y)], fill=(60, 70, 76, 255))
    d.ellipse([8, 14, 56, 62 - 12], fill=(150, 96, 46, 255))                       # copper case
    d.ellipse([12, 18, 52, 46], fill=(20, 70, 70, 255))                            # teal dial
    d.ellipse([16, 22, 48, 42], fill=(30, 140, 130, 255))
    d.polygon([(32, 24), (40, 32), (32, 40), (24, 32)], fill=(200, 255, 230, 255)) # the gator-eye glyph
    d.ellipse([29, 29, 35, 35], fill=(20, 60, 50, 255))
    d.rectangle([54, 26, 60, 34], fill=(200, 140, 70, 255))                        # crown
    return im

def main():
    os.makedirs(f'{R}/textures/entity/alien', exist_ok=True); os.makedirs(f'{R}/textures/gui/alien', exist_ok=True)
    os.makedirs(f'{R}/textures/item', exist_ok=True)
    cases = []; funcs = []
    for spec in ALIENS:
        width, layout = pack(spec)
        funcs.append(java_for(spec, width, layout))
        cases.append('            case %s -> %s();' % (spec['id'].upper(), spec['id']))
        paint(spec, width, layout).save(f"{R}/textures/entity/alien/{spec['id']}.png")
        icon(spec).save(f"{R}/textures/gui/alien/{spec['id']}.png")
        print(spec['id'], width, 'x', width, '->', width * SCALE)
    open(OUT_JAVA, 'w').write(JAVA_HEAD % ('\n'.join(cases), '\n\n'.join(funcs)))
    watch_icon().save(f'{R}/textures/item/swampwatch.png')
    # a preview sheet
    sheet = Image.new('RGBA', (64 * 11 + 20, 80), (40, 40, 48, 255))
    for i, spec in enumerate(ALIENS):
        sheet.paste(icon(spec), (10 + i * 64, 8), icon(spec))
    sheet.paste(watch_icon(), (10 + 10 * 64, 8), watch_icon())
    sheet.save('/tmp/claude-0/-home-claude/3a456fdc-7b05-56c9-bf69-bc83c00ecb1d/scratchpad/alien_icons.png')
    Image.open(f'{R}/textures/entity/alien/mossback.png').save('/tmp/claude-0/-home-claude/3a456fdc-7b05-56c9-bf69-bc83c00ecb1d/scratchpad/mossback_tex.png')

main()

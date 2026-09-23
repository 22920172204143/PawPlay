"""Build reusable vector star/bubble meshes, without copying reference pixels.

Each effect has a fixed pool of independently fading glTF nodes. Both preview
renderers use this same unlit, alpha-blended asset; no material compiler needed.
"""
import json
import math
import colorsys
from pathlib import Path
import struct

ROOT = Path(__file__).resolve().parents[2]
CAPACITY = 24


class Shape:
    def __init__(self):
        self.positions, self.colors, self.indices = [], [], []

    def vertex(self, x, y, brightness, alpha):
        self.positions.append([x, y, 0])
        color = [brightness] * 3 if isinstance(brightness, (int, float)) else list(brightness)
        self.colors.append(color + [alpha])
        return len(self.positions) - 1

    def ring(self, points, brightness, alpha):
        return [self.vertex(x, y, brightness(x, y), alpha(x, y)) for x, y in points]

    def bridge(self, inner, outer):
        for i in range(len(inner)):
            j = (i + 1) % len(inner)
            self.indices += [inner[i], outer[i], outer[j], inner[i], outer[j], inner[j]]


def star():
    s = Shape()
    corners = []
    for i in range(10):
        angle = math.pi / 2 + i * math.pi / 5
        r = .5 if i % 2 == 0 else .245
        corners.append((math.cos(angle) * r, math.sin(angle) * r))
    points = []
    for i, p in enumerate(corners):
        previous, following = corners[i - 1], corners[(i + 1) % 10]
        a = tuple(p[k] * .86 + previous[k] * .14 for k in range(2))
        b = tuple(p[k] * .86 + following[k] * .14 for k in range(2))
        for t in [0, 1/3, 2/3, 1]:
            points.append(tuple((1-t)**2*a[k]+2*t*(1-t)*p[k]+t*t*b[k] for k in range(2)))
    center = s.vertex(0, 0, .90, 1)
    edge = s.ring(points, lambda x, y: .87+y*.24, lambda x, y: .96)
    for i in range(len(edge)):
        s.indices += [center, edge[i], edge[(i+1) % len(edge)]]
    feather = s.ring([(x*1.13, y*1.13) for x, y in points], lambda x, y: .85, lambda x, y: 0)
    s.bridge(edge, feather)
    return s


def bubble():
    s = Shape()
    def linear(rgb):
        return [v/12.92 if v <= .04045 else ((v+.055)/1.055)**2.4 for v in rgb]

    def sheen(x, y):
        # Stylized thin-film colors: a broad colored rim with a different hue
        # on its inner side. Store linear RGB for the shared glTF vertex color.
        radius = math.hypot(x, y)
        hue = (math.atan2(y, x)/math.tau + .10 + (radius-.44)*1.8) % 1
        saturation = .36 + .34*min(1, radius/.47)
        return linear(colorsys.hsv_to_rgb(hue, saturation, 1))

    center = s.vertex(0, 0, linear((.83, .95, 1)), .10)
    previous = None
    segments = 64
    for radius, alpha in [(.24, .12), (.35, .22), (.41, .43), (.449, .78),
                          (.477, 1), (.492, .92), (.511, .27), (.537, 0)]:
        points = [(radius*math.cos(i*math.tau/segments), radius*math.sin(i*math.tau/segments)) for i in range(segments)]
        ring = s.ring(points, sheen, lambda x, y: alpha*(.90+.10*math.sin(math.atan2(y,x)*2)))
        if previous:
            s.bridge(previous, ring)
        else:
            for i in range(segments): s.indices += [center, ring[i], ring[(i+1) % segments]]
        previous = ring

    # Curved white reflections stay legible when a bubble is only 15-25 px wide.
    # The open arcs must not be joined across their endpoints like a closed ring.
    for start, end, radius, width, opacity in [(1.65, 2.72, .402, .041, 1),
                                               (-1.25, -.35, .448, .019, .83)]:
        previous = None
        for offset, strength in [(-1, 0), (-.40, .92), (.30, 1), (1, 0)]:
            row = []
            for i in range(25):
                t = i/24; angle = start+(end-start)*t; r = radius+offset*width
                fade = max(0, math.sin(math.pi*t))**.45
                row.append(s.vertex(r*math.cos(angle), r*math.sin(angle), 1, opacity*strength*fade))
            if previous:
                for i in range(24):
                    s.indices += [previous[i], row[i], row[i+1], previous[i], row[i+1], previous[i+1]]
            previous = row

    for cx, cy, rx, ry, opacity in [(-.18, .235, .047, .079, .94)]:
        middle = s.vertex(cx, cy, 1, opacity)
        border = []
        for i in range(16):
            a = i*math.tau/16
            x, y = rx*math.cos(a), ry*math.sin(a)
            border.append(s.vertex(cx+x*.86-y*.5, cy+x*.5+y*.86, 1, 0))
        for i in range(16): s.indices += [middle, border[i], border[(i+1) % 16]]
    return s


def main():
    gltf = dict(asset=dict(version='2.0', generator='PawPlay vector trail builder'),
                extensionsUsed=['KHR_materials_unlit'], scene=0, scenes=[dict(nodes=[])],
                nodes=[], meshes=[], materials=[], buffers=[], bufferViews=[], accessors=[])
    binary = bytearray()

    def accessor(rows, component, kind):
        flat = [v for row in rows for v in row] if kind != 'SCALAR' else rows
        while len(binary) % 4: binary.append(0)
        start = len(binary)
        binary.extend(struct.pack('<' + ('f' if component == 5126 else 'H')*len(flat), *flat))
        view = len(gltf['bufferViews'])
        gltf['bufferViews'].append(dict(buffer=0, byteOffset=start, byteLength=len(binary)-start))
        a = dict(bufferView=view, componentType=component, count=len(rows), type=kind)
        if kind == 'VEC3':
            a.update(min=[min(v[k] for v in rows) for k in range(3)], max=[max(v[k] for v in rows) for k in range(3)])
        gltf['accessors'].append(a)
        return len(gltf['accessors'])-1

    for style, shape in [('stars', star()), ('bubbles', bubble())]:
        attributes = dict(POSITION=accessor(shape.positions, 5126, 'VEC3'), COLOR_0=accessor(shape.colors, 5126, 'VEC4'))
        indices = accessor(shape.indices, 5123, 'SCALAR')
        for slot in range(CAPACITY):
            name = f'trail_{style}_{slot:02d}'
            material = len(gltf['materials'])
            gltf['materials'].append(dict(name=name, alphaMode='BLEND', doubleSided=True,
                pbrMetallicRoughness=dict(baseColorFactor=[1, 1, 1, 0], metallicFactor=0, roughnessFactor=1),
                extensions=dict(KHR_materials_unlit={})))
            mesh = len(gltf['meshes'])
            gltf['meshes'].append(dict(primitives=[dict(attributes=attributes, indices=indices, material=material)]))
            gltf['scenes'][0]['nodes'].append(len(gltf['nodes']))
            gltf['nodes'].append(dict(name=name, mesh=mesh, scale=[.000001]*3))
    while len(binary) % 4: binary.append(0)
    gltf['buffers'] = [dict(byteLength=len(binary))]
    gltf['extras'] = dict(max_particles=CAPACITY, styles=['stars', 'bubbles'])
    encoded = json.dumps(gltf, separators=(',', ':')).encode()
    encoded += b' ' * ((-len(encoded)) % 4)
    blob = struct.pack('<III', 0x46546C67, 2, 12+8+len(encoded)+8+len(binary))
    blob += struct.pack('<II', len(encoded), 0x4E4F534A)+encoded
    blob += struct.pack('<II', len(binary), 0x004E4942)+binary
    path = ROOT/'app/src/main/assets/models/insect_trail.glb'
    path.write_bytes(blob)
    print(f'{path.name}: {len(blob)} bytes, {CAPACITY} pooled particles per style')


if __name__ == '__main__': main()

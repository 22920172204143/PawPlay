"""Build an editable shallow-volume ladybug and export the same geometry to GLB.

Run with Blender 4.5: blender --background --python tools/art/build_reference_ladybug.py
No geometry or textures are extracted from the reference video.
"""
import argparse
import json
import math
from pathlib import Path
import struct
import sys
import zlib

import bpy
import numpy as np
from mathutils import Vector

ROOT = Path(__file__).resolve().parents[2]
ART = ROOT / 'art/insects/reference_ladybug'
ASSETS = ROOT / 'app/src/main/assets'
REVIEW = ROOT / '.local/reviews/p1'
PROFILE = json.loads((ART / 'model.json').read_text(encoding='utf-8'))


def png(path, rgb):
    """Write deterministic sRGB pixels without external Python packages."""
    rgb = np.asarray(np.clip(rgb, 0, 255), dtype=np.uint8)
    h, w, _ = rgb.shape
    def chunk(kind, data):
        return struct.pack('!I', len(data)) + kind + data + struct.pack('!I', zlib.crc32(kind + data))
    rows = b''.join(b'\x00' + row.tobytes() for row in rgb)
    path.write_bytes(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('!2I5B', w, h, 8, 2, 0, 0, 0))
                     + chunk(b'sRGB', b'\x00') + chunk(b'IDAT', zlib.compress(rows, 9)) + chunk(b'IEND', b''))


def textures():
    folder = ART / 'textures'
    folder.mkdir(parents=True, exist_ok=True)
    n = 1024
    y, x = np.mgrid[0:n, 0:n].astype(np.float32)
    x = x / (n - 1) - 0.5
    y = 0.5 - y / (n - 1)
    base = np.broadcast_to(np.array(PROFILE['shell_srgb'], dtype=float), (n, n, 3)).copy()
    # Broad pigment gradients describe the shallow convex shell without a plastic highlight.
    radial = np.clip(np.hypot(x / .49, y / .50), 0, 1)
    shoulder = np.exp(-(((x + .19) / .36)**2 + ((y - .23) / .40)**2))
    edge = radial**3
    base[:, :, 0] += 15*shoulder - 23*edge - 7*np.clip(-y*2,0,1)
    base[:, :, 1] += 3*shoulder
    base[:, :, 2] += 7*edge - 13*shoulder + 6*np.clip(-y*2,0,1)
    for i, (sx, sy, radius) in enumerate(PROFILE['spots']):
        angle = .31*math.sin(i*1.7)
        dx, dy = x-sx, y-sy
        u = (dx*math.cos(angle)+dy*math.sin(angle))/(radius*(1+.10*math.sin(i*2.3)))
        v = (-dx*math.sin(angle)+dy*math.cos(angle))/(radius*(.92+.08*math.cos(i)))
        d = np.hypot(u,v)
        d *= 1+.045*np.sin(np.arctan2(v,u)*3+i)
        opacity = np.clip((1.20-d)/.49,0,1)*(.75+.07*math.sin(i*1.3))
        tint = np.array(PROFILE['spot_srgb']) + np.array([4*math.sin(i),0,3*math.cos(i)])
        base = base*(1-opacity[:,:,None])+tint*opacity[:,:,None]
    png(folder / 'shell_basecolor.png', base)
    base = np.broadcast_to(np.array(PROFILE['abdomen_srgb'], dtype=float), (n, n, 3)).copy()
    edge = np.clip(np.hypot(x/.48,y/.49),0,1)**2
    base[:,:,1] -= 26*edge + 9*np.clip(-y*2,0,1)
    base[:,:,2] -= 24*edge
    for cx, cy, radius in [(-.14,.23,.11),(.12,.13,.085),(-.035,-.07,.10),(.21,-.20,.11),(-.16,-.30,.08)]:
        d = np.hypot(x-cx, y-cy) / radius
        mask = np.clip((1.02-d) * 13, 0, 1) * .27
        base = base * (1-mask[:, :, None]) + np.array([255,255,131]) * mask[:, :, None]
    png(folder / 'abdomen_basecolor.png', base)


def linear(srgb):
    values = [v/255 for v in srgb]
    return tuple(v/12.92 if v <= .04045 else ((v+.055)/1.055)**2.4 for v in values)


def material(name, color, texture=None, unlit=False):
    mat = bpy.data.materials.new(name)
    mat.use_nodes = True
    tree = mat.node_tree
    tree.nodes.clear()
    output = tree.nodes.new('ShaderNodeOutputMaterial')
    if unlit:
        shader = tree.nodes.new('ShaderNodeRGB')
        shader.outputs[0].default_value = (*linear(color), 1)
        color_input = None
    else:
        shader = tree.nodes.new('ShaderNodeBsdfPrincipled')
        shader.inputs['Base Color'].default_value = (*linear(color), 1)
        shader.inputs['Roughness'].default_value = .83
        shader.inputs['Specular IOR Level'].default_value = .12
        color_input = shader.inputs['Base Color']
    if texture:
        image = bpy.data.images.load(str(texture), check_existing=True)
        node = tree.nodes.new('ShaderNodeTexImage')
        node.image = image
        if unlit:
            tree.links.new(node.outputs['Color'], output.inputs['Surface'])
        else:
            tree.links.new(node.outputs['Color'], color_input)
    if not (unlit and texture):tree.links.new(shader.outputs[0], output.inputs['Surface'])
    mat.diffuse_color = (*linear(color), 1)
    return mat


def empty(name, parent=None, location=(0,0,0)):
    obj = bpy.data.objects.new(name, None)
    bpy.context.collection.objects.link(obj)
    obj.parent = parent
    obj.location = location
    obj.empty_display_type = 'PLAIN_AXES'
    obj.empty_display_size = .1
    return obj


def uv_planar(mesh):
    layer = mesh.uv_layers.new(name='UVMap')
    for poly in mesh.polygons:
        for index in poly.loop_indices:
            co = mesh.vertices[mesh.loops[index].vertex_index].co
            layer.data[index].uv = (co.x + .5, co.y + .5)


def shell(name, side, mat, root):
    radius = PROFILE['shell_radius']
    # Each cap is a closed mesh, with a flat underside and a shallow curved top.
    radial, angular = 20, 64
    vertices = [(0,0,PROFILE['shell_base_height'] + PROFILE['shell_height'])]
    for ring in range(1, radial+1):
        r = math.sin(ring/radial * math.pi/2)
        for j in range(angular+1):
            angle = math.pi/2 + j/angular * math.pi
            x = math.cos(angle) * radius * r * (-side)
            y = math.sin(angle) * radius * r
            z = PROFILE['shell_base_height'] + PROFILE['shell_height'] * math.sqrt(max(0,1-r*r))
            # Low frequency contour variation: broad shoulders, a slightly uneven taper.
            # No random per-vertex noise, so the silhouette stays smooth at phone size.
            if PROFILE.get('contour_asymmetry'):
                x *= 1 + (.026 if side < 0 else -.018)*math.sin(angle*2+.4) + .018*math.cos(angle*3+.7*side)
                y += r*r*(.010*math.cos(angle*2+side*.8) + .006*side)
                z += .003*r*math.sin(angle*2+side)
            vertices.append((x,y,z))
    faces = []
    for j in range(angular):
        faces.append((0,1+j,2+j))
    for ring in range(1, radial):
        a=1+(ring-1)*(angular+1); b=1+ring*(angular+1)
        for j in range(angular):
            faces.append((a+j,b+j,b+j+1,a+j+1))
    boundary = [0] + [1+(r-1)*(angular+1) for r in range(1,radial+1)]
    boundary += [1+(radial-1)*(angular+1)+j for j in range(1,angular+1)]
    boundary += [1+(r-1)*(angular+1)+angular for r in range(radial-1,0,-1)]
    underside=[]
    for index in boundary:
        x,y,_=vertices[index]; underside.append(len(vertices)); vertices.append((x,y,PROFILE['shell_base_height']-.016))
    bottom_center=len(vertices);vertices.append((side*.20,0,PROFILE['shell_base_height']-.016))
    for i,a in enumerate(boundary):
        next_i=(i+1)%len(boundary)
        faces.append((a,underside[i],underside[next_i],boundary[next_i]))
        faces.append((bottom_center,underside[next_i],underside[i]))
    mesh=bpy.data.meshes.new(name);mesh.from_pydata(vertices,[],faces);mesh.update()
    uv_planar(mesh)
    obj=bpy.data.objects.new(name,mesh);bpy.context.collection.objects.link(obj)
    obj.data.materials.append(mat)
    # Recalculate normals instead of depending on mirrored winding order.
    bpy.context.view_layer.objects.active=obj;obj.select_set(True)
    bpy.ops.object.mode_set(mode='EDIT');bpy.ops.mesh.select_all(action='SELECT');bpy.ops.mesh.normals_make_consistent(inside=False);bpy.ops.object.mode_set(mode='OBJECT')
    obj.select_set(False)
    for polygon in mesh.polygons: polygon.use_smooth=True
    pivot=empty(name+'_hinge',root,(0,PROFILE['shell_hinge_y'],0))
    obj.parent=pivot;obj.location.y=-PROFILE['shell_hinge_y']
    pivot.rotation_euler.z=side*math.radians(PROFILE['shell_open_degrees'])
    return pivot


def ellipsoid(name, location, scale, mat, parent, planar=False):
    bpy.ops.mesh.primitive_uv_sphere_add(segments=48,ring_count=24,location=location)
    obj=bpy.context.object;obj.name=name;obj.scale=scale
    bpy.ops.object.transform_apply(location=False,rotation=False,scale=True)
    obj.parent=parent;obj.data.materials.append(mat)
    if planar:
        for layer in list(obj.data.uv_layers):obj.data.uv_layers.remove(layer)
        uv_planar(obj.data)
    for p in obj.data.polygons:p.use_smooth=True
    obj.select_set(False)
    return obj


def antenna(name,side,mat,root):
    pivot=empty(name+'_pivot',root,(side*.105,.555,.145))
    curve=bpy.data.curves.new(name,'CURVE');curve.dimensions='3D'
    curve.resolution_u=12;curve.bevel_depth=.0065;curve.bevel_resolution=3
    spline=curve.splines.new('BEZIER')
    coords=[(.105,.555),(.145,.69),(.19,.82),(.29,.865),(.39,.815),(.39,.715),(.33,.686),(.278,.733),(.288,.781)]
    if PROFILE.get('contour_asymmetry'):
        coords = ([(.105,.555),(.157,.690),(.216,.819),(.318,.873),(.422,.807),(.404,.702),(.338,.674),(.283,.719),(.286,.768)]
                  if side < 0 else
                  [(.105,.555),(.127,.687),(.172,.824),(.253,.884),(.331,.853),(.350,.784),(.311,.746),(.271,.765),(.278,.803)])
    spline.bezier_points.add(len(coords)-1)
    for point,(x,y) in zip(spline.bezier_points,coords):
        point.co=(side*(x-.105),y-.555, .013*math.sin((y-.555)*7))
        point.handle_left_type=point.handle_right_type='AUTO'
    obj=bpy.data.objects.new(name,curve);bpy.context.collection.objects.link(obj);obj.parent=pivot
    obj.data.materials.append(mat)
    bpy.context.view_layer.objects.active=obj;obj.select_set(True);bpy.ops.object.convert(target='MESH');obj.select_set(False)
    return pivot


def aim(obj, target):
    obj.rotation_euler=(Vector(target)-obj.location).to_track_quat('-Z','Y').to_euler()


def build():
    bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False)
    textures()
    # Unlit, authored surface color is intentional for this nearly flat-lit reference.
    # Geometry remains fully three-dimensional and can be relit by replacing materials.
    shell_mat=material('shell_red_spotted',PROFILE['shell_srgb'],ART/'textures/shell_basecolor.png',unlit=True)
    abdomen_mat=material('abdomen_yellow',PROFILE['abdomen_srgb'],ART/'textures/abdomen_basecolor.png',unlit=True)
    gold=material('head_gold',[255,225,64],ART/'textures/abdomen_basecolor.png',unlit=True)
    antenna_mat=material('antenna_gold',PROFILE['antenna_srgb'],unlit=True)
    eye_mat=material('eyes_teal',[32,146,168],unlit=True)
    root=empty('ladybug_root')
    ellipsoid('abdomen',(0,0,.083),(.463,.468,.09),abdomen_mat,root,planar=True)
    ellipsoid('head',(0,.478,.11),(.179,.145,.066),gold,root,planar=True)
    for side in [-1,1]:
        ellipsoid('eye_left' if side<0 else 'eye_right',(side*.065,.581,.159),(.015,.019,.007),eye_mat,root)
        antenna('antenna_left' if side<0 else 'antenna_right',side,antenna_mat,root)
        shell('shell_left' if side<0 else 'shell_right',side,shell_mat,root)
    return root


def export_model(root):
    (ASSETS/'models').mkdir(parents=True,exist_ok=True)
    (ASSETS/'profiles').mkdir(parents=True,exist_ok=True)
    bpy.ops.object.select_all(action='DESELECT')
    root.select_set(True)
    for child in root.children_recursive:child.select_set(True)
    bpy.context.view_layer.objects.active=root
    bpy.ops.export_scene.gltf(filepath=str(ASSETS/'models/reference_ladybug.glb'),export_format='GLB',use_selection=True,
                              export_animations=False,export_yup=True,export_cameras=False,export_lights=False)
    (ASSETS/'profiles/reference_ladybug.json').write_text(json.dumps(PROFILE,indent=2)+'\n',encoding='utf-8')


def setup_review(root):
    scene=bpy.context.scene
    scene.render.engine='CYCLES'
    scene.cycles.device='CPU';scene.cycles.samples=24;scene.cycles.use_denoising=False
    scene.render.resolution_x=960;scene.render.resolution_y=960;scene.render.resolution_percentage=100
    scene.render.image_settings.file_format='PNG';scene.render.image_settings.color_mode='RGBA'
    scene.render.film_transparent=True
    scene.view_settings.view_transform='Standard';scene.view_settings.look='None'
    scene.view_settings.exposure=0;scene.view_settings.gamma=1
    scene.world.color=(.2,.2,.2)
    bpy.ops.object.camera_add(location=(0,.14,5))
    camera=bpy.context.object;camera.name='ReviewCamera';camera.data.type='ORTHO';camera.data.ortho_scale=1.70
    aim(camera,(0,.14,0));scene.camera=camera
    return camera


def export_stage():
    image=bpy.data.images.load(str(ROOT/'app/src/main/res/drawable-nodpi/bg_mulch.jpg'))
    pixels=np.array(image.pixels[:]).reshape(image.size[1],image.size[0],4)
    stage_path=ART/'textures/stage_basecolor.png'
    png(stage_path,np.flipud(pixels[:,:,:3])*np.array([.42,.40,.36])*255)
    mat=material('ground_mulch',[255,255,255],stage_path,unlit=True)
    bpy.ops.mesh.primitive_plane_add(size=64,location=(0,0,-.025))
    obj=bpy.context.object;obj.name='reference_ground';obj.data.materials.append(mat)
    for uv in obj.data.uv_layers.active.data:uv.uv=((uv.uv.x-.5)*6.4+.5,(uv.uv.y-.5)*6.4+.5)
    bpy.ops.object.select_all(action='DESELECT');obj.select_set(True)
    bpy.ops.export_scene.gltf(filepath=str(ASSETS/'models/reference_stage.glb'),export_format='GLB',use_selection=True,
                              export_animations=False,export_yup=True)
    obj.hide_render=True;obj.hide_set(True);obj.select_set(False)


def main():
    parser=argparse.ArgumentParser();parser.add_argument('--skip-render',action='store_true')
    args=parser.parse_args(sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else [])
    REVIEW.mkdir(parents=True,exist_ok=True)
    root=build();export_model(root);export_stage();camera=setup_review(root)
    bpy.ops.wm.save_as_mainfile(filepath=str(ART/'reference_ladybug.blend'))
    if not args.skip_render:
        for name,heading,opening in [('front',0,13),('right',-90,7),('back',180,20),('left',90,14)]:
            root.rotation_euler.z=math.radians(heading)
            for side,label in [(-1,'left'),(1,'right')]:bpy.data.objects['shell_'+label+'_hinge'].rotation_euler.z=side*math.radians(opening)
            bpy.context.scene.render.filepath=str(REVIEW/f'model-{name}.png');bpy.ops.render.render(write_still=True)
        root.rotation_euler.z=0
        camera.location=(1.5,-2.3,2.4);aim(camera,(0,.12,.08));camera.data.ortho_scale=1.65
        bpy.context.scene.render.filepath=str(REVIEW/'model-volume.png');bpy.ops.render.render(write_still=True)
    print('PAWPLAY_MODEL_READY',ASSETS/'models/reference_ladybug.glb')


if __name__=='__main__':main()

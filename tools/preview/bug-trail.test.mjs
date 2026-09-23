import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {BugMotion} from './bug-motion.mjs';
import {BugTrail} from './bug-trail.mjs';
const profile=JSON.parse(readFileSync(new URL('../../art/insects/reference_ladybug/model.json',import.meta.url)));
const pose=(speed=0)=>({x:0,z:0,height:.018,yaw:0,scaleZ:1,speed});

test('trail positions, lifetimes and emission are identical at 30/60/120 fps',()=>{
  const run=fps=>{
    const m=new BugMotion(profile.locomotion),t=new BugTrail(profile.trail);m.select('dash');
    for(let i=0;i<fps*24;i++)m.advance(1/fps,p=>t.step(p));
    return JSON.stringify({pose:m.pose,spawned:t.spawned,particles:t.particles});
  };
  assert.equal(run(30),run(120));assert.equal(run(60),run(120));
});

test('switching effects and pausing never changes the approved motion',()=>{
  const m=new BugMotion(profile.locomotion),control=new BugMotion(profile.locomotion),t=new BugTrail(profile.trail);
  for(let i=0;i<3600;i++){
    if(i%600===0)t.select(['stars','bubbles','none'][Math.floor(i/600)%3]);
    const dt=i>=900&&i<960?0:1/60;
    const before=JSON.stringify(t.particles);
    m.advance(dt,p=>t.step(p));control.advance(dt);
    assert.deepEqual(m.pose,control.pose);
    if(dt===0)assert.equal(JSON.stringify(t.particles),before);
  }
});

test('slow travel emits fewer particles, idle stops emission and the tail fully expires',()=>{
  function counts(speed){
    const t=new BugTrail(profile.trail);let maxVisible=0;
    for(let i=0;i<1200;i++){
      t.step(pose(speed));maxVisible=Math.max(maxVisible,t.particles.filter(p=>p.active).length);
      for(const p of t.particles.filter(p=>p.active))assert.ok(p.alpha>=0&&p.alpha<=1&&p.size>0);
    }
    return {t,maxVisible};
  }
  assert.equal(counts(0).t.spawned,0);
  const slow=counts(.3),fast=counts(2.5);
  assert.ok(fast.t.spawned>slow.t.spawned*4);
  assert.ok(fast.maxVisible>=5&&fast.maxVisible<=10,'a fuller short tail stays well below the pool capacity');
  const n=fast.t.spawned;
  for(let i=0;i<240;i++)fast.t.step(pose());
  assert.equal(fast.t.spawned,n);assert.ok(fast.t.particles.every(p=>!p.active));
});

test('existing particles stay on the old path when the insect moves and turns away',()=>{
  const t=new BugTrail(profile.trail);
  while(t.spawned===0)t.step(pose(2.5));
  const particle=t.particles.find(p=>p.active),oldX=particle.x,oldZ=particle.z;
  assert.ok(particle.z>.5,'emits at the rear of the forward-facing insect');
  t.step({...pose(),x:10,z:10,yaw:Math.PI});
  assert.ok(Math.abs(particle.x-oldX)<.01&&Math.abs(particle.z-oldZ)<.01,'world-space trail does not follow the rig');
});

test('bubbles rise and expand, stars shrink, both fade with a bounded reusable pool',()=>{
  for(const style of ['stars','bubbles']){
    const t=new BugTrail({...profile.trail,style,max_particles:2});
    while(t.spawned===0)t.step(pose(2.5));
    const p=t.particles.find(p=>p.active);
    while(p.age<p.life*.30)t.step(pose());
    const size=p.size,alpha=p.alpha,y=p.y;
    while(p.age<p.life*.80)t.step(pose());
    assert.ok(p.alpha<alpha);assert.ok(p.y>y);
    assert.ok(style==='bubbles'?p.size>size:p.size<size);
    for(let i=0;i<120*30;i++)t.step(pose(2.5));
    assert.equal(t.particles.length,2);assert.ok(t.spawned>20);
    t.select('none');assert.ok(t.particles.every(p=>!p.active));
    assert.throws(()=>t.select('missing'));
  }
});

test('shared effect GLB contains independent transparent slots for every configured style',()=>{
  const bytes=readFileSync(new URL('../../app/src/main/assets/models/insect_trail.glb',import.meta.url));
  const gltf=JSON.parse(bytes.toString('utf8',20,20+bytes.readUInt32LE(12)));
  assert.ok(profile.trail.max_particles<=gltf.extras.max_particles);
  const materials=new Set();
  for(const style of Object.keys(profile.trail.styles))for(let i=0;i<profile.trail.max_particles;i++){
    const node=gltf.nodes.find(n=>n.name===`trail_${style}_${String(i).padStart(2,'0')}`);
    assert.ok(node);const primitive=gltf.meshes[node.mesh].primitives[0];
    materials.add(primitive.material);const material=gltf.materials[primitive.material];
    assert.equal(material.alphaMode,'BLEND');assert.ok(material.extensions.KHR_materials_unlit);
    assert.ok(Number.isInteger(primitive.attributes.COLOR_0));
  }
  assert.equal(materials.size,profile.trail.max_particles*Object.keys(profile.trail.styles).length);
});

import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {BugMotion} from './bug-motion.mjs';
const config=JSON.parse(readFileSync(new URL('../../art/insects/reference_ladybug/model.json',import.meta.url))).locomotion;
const values=p=>[p.x,p.z,p.speed,p.yaw,p.pitch,p.roll,p.wingLeft,p.wingRight,...p.antennae,p.scaleX,p.scaleY,p.scaleZ];

test('navigation and poses replay identically at 30, 60 and 120 fps',()=>{
  const results=[30,60,120].map(fps=>{
    const m=new BugMotion(config);
    for(let i=0;i<fps*60;i++)m.advance(1/fps);
    return values(m.pose);
  });
  results.slice(1).forEach(sample=>sample.forEach((v,i)=>assert.ok(Math.abs(v-results[0][i])<1e-8)));
});

test('multi-seed navigation stays inside the arena without teleporting or abrupt speed changes',()=>{
  for(const seed of [42,53,77,101,230924]){
    const m=new BugMotion({...config,seed});let before=structuredClone(m.pose);
    for(let i=0;i<120*120;i++){
      const p=m.advance(1/120);
      assert.ok(Math.hypot(p.x,p.z)<config.arena_radius);
      assert.ok(Math.hypot(p.x-before.x,p.z-before.z)<=config.max_speed/120+1e-8);
      assert.ok(Math.abs(p.speed-before.speed)<=Math.max(config.acceleration,config.braking)/120+1e-8);
      assert.ok(p.wingLeft>=0&&p.wingLeft<=config.max_open_degrees);
      assert.ok(p.wingRight>=0&&p.wingRight<=config.max_open_degrees);
      assert.ok(p.antennae.every(v=>Number.isFinite(v)&&Math.abs(v)<35));
      assert.ok([p.scaleX,p.scaleY,p.scaleZ].every(v=>Number.isFinite(v)&&v>.90&&v<1.10));
      assert.ok(Math.abs(p.scaleX*p.scaleY*p.scaleZ-1)<1e-12,'body preserves volume');
      before={x:p.x,z:p.z,speed:p.speed};
    }
  }
});

test('fast travel increases wing rate, body swing and independent antenna movement together',()=>{
  function measure(mode){
    const m=new BugMotion(config);m.select(mode);
    let samples=0,speed=0,wing=0,body=0,antenna=0,independent=0;
    for(let i=0;i<60*40;i++){
      const p=m.advance(1/60);if(i<60*5)continue;
      samples++;speed+=p.speed;wing+=p.wingHz;body+=p.swayAmplitude;
      antenna+=p.antennae.reduce((sum,v)=>sum+v*v,0);
      independent+=Math.abs(p.antennae[0]-p.antennae[3]);
    }
    return {speed:speed/samples,wing:wing/samples,body:body/samples,antenna:Math.sqrt(antenna/samples),independent:independent/samples};
  }
  const slow=measure('cruise'),fast=measure('dash');
  assert.ok(fast.speed>slow.speed*4);
  assert.ok(fast.wing>slow.wing*2);
  assert.ok(fast.body>slow.body*3);
  assert.ok(fast.antenna>slow.antenna*2);
  assert.ok(fast.independent>slow.independent);
});

test('pause freezes the entire insect; forced state changes and manual pose do not teleport',()=>{
  const m=new BugMotion(config);for(let i=0;i<360;i++)m.advance(1/60);
  const old=structuredClone(m.pose);for(let i=0;i<120;i++)assert.deepEqual(m.advance(0),old);
  m.select('dash');assert.deepEqual(m.pose,old);
  m.setRestOpening(38);assert.equal(m.pose.x,old.x);assert.equal(m.pose.z,old.z);
  assert.equal(m.pose.wingLeft,38);
  assert.ok(Math.abs(m.advance(1/120).wingLeft-38)<.05);
  assert.throws(()=>m.advance(-1));assert.throws(()=>m.select('missing'));
});

test('natural travel includes slow travel, bursts, acceleration and deceleration',()=>{
  const m=new BugMotion(config),labels=new Set(),states=new Set();
  for(let i=0;i<60*90;i++){const p=m.advance(1/60);labels.add(p.label);states.add(p.activity)}
  assert.deepEqual(states,new Set(['cruise','dash','pause']));
  assert.ok(labels.has('加速')&&labels.has('减速'));
});

test('released antenna tips overshoot visibly, lag behind the root and settle without sustained ringing',()=>{
  const released=new BugMotion(config),control=new BugMotion(config);
  // Displace each local section by ten degrees. Subtract the identical moving
  // control to isolate passive recoil from the insect's ongoing motion.
  released.pose.antennae.fill(10);
  const minimum=Array(6).fill(10),peakTime=Array(6).fill(0);
  for(let frame=1;frame<=6*120;frame++){
    released.advance(1/120);control.advance(1/120);
    released.pose.antennae.forEach((v,i)=>{
      const difference=v-control.pose.antennae[i];
      if(difference<minimum[i]){minimum[i]=difference;peakTime[i]=frame/120}
    });
  }
  for(const side of [0,3]){
    assert.ok(minimum[side]*config.antenna_root.bend_weights[0]>-1.2,'attachment carries a small share of the bend');
    assert.ok(minimum[side+1]<-2.5,'middle bends back past its neutral position');
    assert.ok(minimum[side+2]<-3.5,'tip has a clearly visible reverse swing');
    assert.ok(peakTime[side+2]>peakTime[side]+.15,'tip responds later than attachment');
  }
  released.pose.antennae.forEach((v,i)=>assert.ok(Math.abs(v-control.pose.antennae[i])<.03,'recoil dies away'));
});

test('fast strokes close back down, spread fully and reach the reference cadence',()=>{
  const m=new BugMotion(config);m.select('dash');
  let cycle=-1,sample,cycles=[];
  for(let i=0;i<120*60;i++){
    const p=m.advance(1/120),next=Math.floor(m.phase);
    if(next!==cycle){if(sample)cycles.push(sample);cycle=next;sample={lo:55,hi:0,minSpeed:Infinity,hz:0}}
    sample.lo=Math.min(sample.lo,p.wingLeft);sample.hi=Math.max(sample.hi,p.wingLeft);
    sample.minSpeed=Math.min(sample.minSpeed,p.speed);sample.hz=p.wingHz;
  }
  const fast=cycles.filter(c=>c.minSpeed>config.max_speed*.72);
  assert.ok(fast.length>15,'inspect multiple complete fast cycles');
  assert.ok(fast.every(c=>c.hi-c.lo>18),'fast cycles have real opening excursion');
  assert.ok(fast.every(c=>c.lo<13),'opening never becomes a static wide hold');
  assert.ok(fast.some(c=>c.hi>43),'large strokes expose the abdomen');
  assert.ok(fast.some(c=>c.hz>=9.5),'fastest motion approaches ten cycles per second');
});

test('body elasticity is subtle while slow, stronger while fast, and settles after braking',()=>{
  function deformation(mode){
    const m=new BugMotion(config);m.select(mode);let total=0,n=0;
    for(let i=0;i<120*30;i++){
      const p=m.advance(1/120);if(i<600)continue;
      total+=(p.scaleX-1)**2+(p.scaleZ-1)**2;n++;
    }
    return Math.sqrt(total/n);
  }
  assert.ok(deformation('dash')>deformation('cruise')*3);
  const m=new BugMotion(config);m.select('dash');for(let i=0;i<360;i++)m.advance(1/120);
  const before=values(m.pose);m.select('pause');assert.deepEqual(values(m.pose),before);
  for(let i=0;i<120*6;i++)m.advance(1/120);
  assert.ok(Math.abs(m.pose.scaleX-1)<.008&&Math.abs(m.pose.scaleZ-1)<.008,'idle does not keep pumping');
});

test('basal stems keep bending during steady fast travel instead of freezing at wingbeat frequency',()=>{
  const c={...config,arena_radius:200,states:config.states.map(s=>({...s,
    speed:s.id==='dash'?[2.5,2.5]:s.speed,duration:[40,40]}))};
  const m=new BugMotion(c);m.goalX=0;m.goalZ=-100;m.select('dash');
  const lo=[Infinity,Infinity],hi=[-Infinity,-Infinity];
  for(let frame=0;frame<20*120;frame++){
    const p=m.advance(1/120);if(frame<5*120)continue;
    assert.ok(Math.abs(m.acceleration)<.001&&Math.abs(m.turnRate)<.001);
    [p.antennae[0],p.antennae[3]].forEach((v,i)=>{lo[i]=Math.min(lo[i],v);hi[i]=Math.max(hi[i],v)});
  }
  lo.forEach((v,i)=>assert.ok(hi[i]-v>40,'both stems have a large sweep while travel is steady'));
  const weights=config.antenna_root.bend_weights;
  assert.equal(weights.length,4);assert.ok(weights.every(v=>v>0&&v<=.35));
  assert.ok(Math.abs(weights.reduce((a,b)=>a+b,0)-1)<1e-12,'distributed bend preserves the intended total angle');
});

test('exported antenna stems contain the four-section chain, with the old attachment points preserved',()=>{
  const bytes=readFileSync(new URL('../../app/src/main/assets/models/reference_ladybug.glb',import.meta.url));
  const gltf=JSON.parse(bytes.toString('utf8',20,20+bytes.readUInt32LE(12)));
  const parent=new Map();gltf.nodes.forEach((n,i)=>(n.children??[]).forEach(child=>parent.set(child,i)));
  const id=name=>{const i=gltf.nodes.findIndex(n=>n.name===name);assert.ok(i>=0,`missing ${name}`);return i};
  const origin=index=>{const n=gltf.nodes[index],p=parent.has(index)?origin(parent.get(index)):[0,0,0];
    assert.ok(!n.rotation||n.rotation.every((v,i)=>Math.abs(v-(i===3?1:0))<1e-6),'rest chain is translation-only');
    return p.map((v,i)=>v+(n.translation?.[i]??0))};
  const profile=JSON.parse(readFileSync(new URL('../../art/insects/reference_ladybug/model.json',import.meta.url)));
  for(const [side,sign] of [['left',-1],['right',1]]){
    const names=[`antenna_${side}_pivot`,...[1,2,3].map(n=>`antenna_${side}_root_flex_${n}`),
      `antenna_${side}_mid_pivot`,`antenna_${side}_tip_pivot`];
    names.slice(1).forEach((name,i)=>assert.equal(parent.get(id(name)),id(names[i])));
    for(const [name,point] of [[names[0],0],[names[4],2],[names[5],4]]){
      const [x,y]=profile[`antenna_${side}_points`][point];
      const rest=origin(id(name));assert.ok(Math.abs(rest[0]-sign*x)<1e-6&&Math.abs(rest[2]+y)<1e-6);
    }
  }
});

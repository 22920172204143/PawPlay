import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {WingMotion} from './wing-motion.mjs';

const config=JSON.parse(readFileSync(new URL('../../art/insects/reference_ladybug/model.json',import.meta.url))).motion;
const poseValues=p=>[p.left,p.right,p.antennaLeft,p.antennaRight];

test('30, 60, 120 fps produce the same pose and mode after 30 seconds',()=>{
  const samples=[30,60,120].map(fps=>{
    const motion=new WingMotion(config);
    for(let i=0;i<fps*30;i++)motion.advance(1/fps);
    return motion.pose;
  });
  for(const sample of samples.slice(1)){
    assert.equal(sample.label,samples[0].label);
    poseValues(sample).forEach((v,i)=>assert.ok(Math.abs(v-poseValues(samples[0])[i])<1e-8));
  }
});

test('five minutes remain bounded, continuous and visit every motion mode',()=>{
  const motion=new WingMotion(config),seen=new Set();let maxJump=0;
  for(let i=0;i<60*300;i++){
    const previous=poseValues(motion.pose),p=motion.advance(1/60);
    seen.add(p.label);
    for(const [index,value] of [p.left,p.right].entries()){
      assert.ok(Number.isFinite(value)&&value>=0&&value<=config.max_open_degrees);
      maxJump=Math.max(maxJump,Math.abs(previous[index]-value));
    }
  }
  assert.equal(seen.size,config.modes.length);
  assert.ok(maxJump<9,`pose discontinuity: ${maxJump}`);
});

test('short bursts are visibly faster than gentle motion',()=>{
  function peaks(mode){
    const motion=new WingMotion(config);motion.select(mode);
    let prior=0,slope=0,count=0;
    for(let i=0;i<120*12;i++){
      const current=motion.advance(1/120).left,delta=current-prior;
      if(i>240&&slope>0&&delta<0)count++;
      if(delta!==0)slope=delta;
      prior=current;
    }
    return count;
  }
  assert.ok(peaks('flutter')>peaks('gentle')*1.7);
});

test('manual pose and pause hold still; mode changes blend from the current pose',()=>{
  const motion=new WingMotion(config);motion.setRestOpening(38);
  const before=poseValues(motion.pose);
  for(let i=0;i<200;i++)assert.deepEqual(poseValues(motion.advance(0)),before);
  motion.select('flutter');assert.deepEqual(poseValues(motion.pose),before);
  assert.ok(Math.abs(motion.advance(1/120).left-38)<.1);
  for(let i=0;i<240;i++)motion.advance(1/120);
  const paused=poseValues(motion.pose);assert.deepEqual(poseValues(motion.advance(0)),paused);
  assert.throws(()=>motion.select('missing'));
  assert.throws(()=>motion.advance(NaN));
});

test('per-insect seeds vary the motion but replay deterministically',()=>{
  const a=new WingMotion(config),b=new WingMotion(config),c=new WingMotion({...config,seed:42});
  for(let i=0;i<600;i++){a.advance(1/60);b.advance(1/60);c.advance(1/60)}
  assert.deepEqual(poseValues(a.pose),poseValues(b.pose));
  assert.notDeepEqual(poseValues(a.pose),poseValues(c.pose));
});

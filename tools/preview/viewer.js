import * as THREE from 'three';
import {BugMotion} from './bug-motion.mjs';
import {GLTFLoader} from 'three/addons/loaders/GLTFLoader.js';

const viewport=document.querySelector('#viewport'),status=document.querySelector('#status');
const renderer=new THREE.WebGLRenderer({antialias:true,alpha:false,preserveDrawingBuffer:true});
renderer.setPixelRatio(Math.min(devicePixelRatio,2));renderer.outputColorSpace=THREE.SRGBColorSpace;
renderer.toneMapping=THREE.NoToneMapping;viewport.append(renderer.domElement);
const scene=new THREE.Scene();scene.background=new THREE.Color('#202820');
const camera=new THREE.OrthographicCamera(-1.3,1.3,1.3,-1.3,.01,100),loader=new GLTFLoader();
const antennaNames=['antenna_left_pivot','antenna_left_mid_pivot','antenna_left_tip_pivot',
  'antenna_right_pivot','antenna_right_mid_pivot','antenna_right_tip_pivot'];
const rootFlexNames=['left','right'].map(side=>[1,2,3].map(part=>`antenna_${side}_root_flex_${part}`));
let profile,bug,root,left,right,antennae,rootFlex,motion,previousMotion,previousRootWeights;
let showingPrevious=false,mode='front',playing=false,elapsed=0,lastTime=0,opening=13;
let reboundDemo=false,demoTime=0,demoStage=0;
let elasticEnabled=true;
const controls=[...document.querySelectorAll('.controls button,.controls select,.controls input')];
controls.forEach(control=>control.disabled=true);
const referenceVideo=document.querySelector('#reference-motion');
document.querySelector('#reference-clip').onchange=async e=>{
  const id=e.target.value,still=id==='still';
  referenceVideo.pause();referenceVideo.hidden=still;document.querySelector('#reference-still').hidden=!still;
  const label=document.querySelector('#reference-status');
  label.textContent=({still:'13 秒 · 静态造型','fast-wide':'1.0–2.3 秒 · 原速','fast-narrow':'19.0–20.3 秒 · 原速',slow:'5.2–6.5 秒 · 原速'})[id];
  if(!still){
    referenceVideo.src='/.local/reviews/p1/reference-r6/'+id+'.mp4';
    try{await referenceVideo.play()}catch{label.textContent='请点播放；本地片段可能缺失'}
  }
};
window.reviewState={loaded:false,frames:0,mode};
function resize(){renderer.setSize(viewport.clientWidth,viewport.clientHeight,false)}
new ResizeObserver(resize).observe(viewport);
function bindParts(){
  root=bug.getObjectByName('ladybug_root');left=bug.getObjectByName('shell_left_hinge');right=bug.getObjectByName('shell_right_hinge');
  antennae=antennaNames.map(name=>bug.getObjectByName(name));
  rootFlex=rootFlexNames.map(names=>names.map(name=>bug.getObjectByName(name)));
  if(!root||!left||!right||[...antennae,...rootFlex.flat()].some(x=>!x))throw Error('模型部件不完整');
}
function updateCamera(){
  const extent=mode==='scale'?4.58:mode==='antenna'?.73:1.30;
  camera.left=-extent;camera.right=extent;camera.top=extent;camera.bottom=-extent;
  const track=mode!=='scale';
  let x=track?motion.pose.x:0,z=track?motion.pose.z:0;
  if(mode==='antenna'){
    const yaw=motion.pose.yaw,distance=.60*(elasticEnabled?motion.pose.scaleZ:1);
    x-=Math.sin(yaw)*distance;z-=Math.cos(yaw)*distance;
    camera.up.set(-Math.sin(yaw),0,-Math.cos(yaw));camera.position.set(x,5,z+.00001);
    camera.lookAt(x,.08,z);camera.updateProjectionMatrix();return;
  }
  if(mode==='volume'){
    const angle=elapsed*.55;camera.up.set(0,1,0);camera.position.set(x+Math.sin(angle)*3,3.2,z+Math.cos(angle)*3);
  }else{camera.up.set(0,0,-1);camera.position.set(x,5,z+.00001)}
  camera.lookAt(x,.08,z);camera.updateProjectionMatrix();
}
function selectMode(value){
  mode=value;window.reviewState.mode=mode;
  if(mode==='volume')setPlaying(true);
  for(const id of ['front','antenna','volume','scale'])document.getElementById(id).classList.toggle('selected',id===value);
  updateCamera();
}
function setPlaying(value){playing=value;document.querySelector('#play').textContent=playing?'暂停动作':'播放动作'}
for(const id of ['front','antenna','volume','scale'])document.getElementById(id).onclick=()=>selectMode(id);
document.querySelector('#play').onclick=()=>setPlaying(!playing);
document.querySelector('#elastic').onchange=e=>{elasticEnabled=e.target.checked};
document.querySelector('#opening').oninput=e=>{
  setPlaying(false);opening=Number(e.target.value);motion?.setRestOpening(opening);previousMotion?.setRestOpening(opening);
};
function selectMotion(id){motion.select(id);previousMotion?.select(id)}
document.querySelector('#motion').onchange=e=>{
  if(!motion)return;
  reboundDemo=e.target.value==='rebound';demoTime=0;demoStage=0;
  selectMotion(reboundDemo?'dash':e.target.value);setPlaying(true);
};
function frame(now){
  const dt=lastTime?Math.min((now-lastTime)/1000,.05):0;lastTime=now;
  if(playing){
    elapsed+=dt;
    if(reboundDemo){
      demoTime+=dt;
      // Three seconds of travel, then five seconds to see braking and ring-down.
      const stage=demoTime%8<3?0:1;
      if(stage!==demoStage){demoStage=stage;selectMotion(stage===0?'dash':'pause')}
    }
    motion.advance(dt);previousMotion?.advance(dt);
  }
  if(bug){
    const p=showingPrevious?previousMotion.pose:motion.pose;
    root.position.set(p.x,p.height,p.z);root.rotation.set(p.pitch,p.yaw,p.roll,'YXZ');
    root.scale.set(elasticEnabled?(p.scaleX??1):1,elasticEnabled?(p.scaleY??1):1,elasticEnabled?(p.scaleZ??1):1);
    left.rotation.y=-THREE.MathUtils.degToRad(p.wingLeft);right.rotation.y=THREE.MathUtils.degToRad(p.wingRight);
    antennae.forEach((node,i)=>node.rotation.y=THREE.MathUtils.degToRad(p.antennae[i]));
    const weights=showingPrevious?previousRootWeights:profile.locomotion.antenna_root.bend_weights;
    rootFlex.forEach((nodes,side)=>{
      const angle=THREE.MathUtils.degToRad(p.antennae[side*3]);
      antennae[side*3].rotation.y=angle*weights[0];
      nodes.forEach((node,i)=>node.rotation.y=angle*weights[i+1]);
    });
    opening=(p.wingLeft+p.wingRight)/2;
    document.querySelector('#motion-state').textContent=playing?p.label:'动作已暂停';
    document.querySelector('#speed').textContent=(p.speed/1.1).toFixed(1)+' 个体长/秒';
    document.querySelector('#opening').value=opening;document.querySelector('#angle').value=opening.toFixed(1)+'°';
    updateCamera();renderer.render(scene,camera);window.reviewState.frames++;
  }
  requestAnimationFrame(frame);
}
try{
  profile=await fetch('/app/src/main/assets/profiles/reference_ladybug.json').then(r=>{if(!r.ok)throw Error(r.status);return r.json()});
  motion=new BugMotion(profile.locomotion,profile.shell_open_degrees);
  // Optional local snapshot: identical navigation/model, earlier local animation.
  // A clean checkout works without the review evidence directory.
  try{
    const baseline='/.local/reviews/p1/before-amplitude-r7/';
    const [source,oldProfile]=await Promise.all([import(baseline+'bug-motion.mjs'),
      fetch(baseline+'profile.json').then(r=>{if(!r.ok)throw Error(r.status);return r.json()})]);
    previousMotion=new source.BugMotion(oldProfile.locomotion,oldProfile.shell_open_degrees);
    previousRootWeights=oldProfile.locomotion.antenna_root.bend_weights;
    const compare=document.querySelector('#compare');
    compare.onclick=()=>{
      showingPrevious=!showingPrevious;
      compare.textContent=showingPrevious?'返回本轮动作':'查看调整前动作';
      status.textContent=showingPrevious?'调整前 · 根部摆幅较小':'本轮 · 加大根部摆幅';
      document.querySelector('#elastic').disabled=showingPrevious;
    };
  }catch{document.querySelector('#compare').hidden=true}
  const [model,stage]=await Promise.all([loader.loadAsync('/app/src/main/assets/models/reference_ladybug.glb'),loader.loadAsync('/app/src/main/assets/models/reference_stage.glb')]);
  bug=model.scene;bindParts();scene.add(stage.scene,bug);updateCamera();resize();
  controls.forEach(control=>control.disabled=false);
  window.reviewState.loaded=true;status.textContent='本轮 · 加大根部摆幅';
  const params=new URLSearchParams(location.search);if(params.has('view'))selectMode(params.get('view'));
  setPlaying(!matchMedia('(prefers-reduced-motion: reduce)').matches);requestAnimationFrame(frame);
}catch(error){status.textContent='加载失败：'+error.message;window.reviewState.error=error.message;console.error(error)}

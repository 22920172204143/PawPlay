import * as THREE from 'three';
import {GLTFLoader} from 'three/addons/loaders/GLTFLoader.js';

const viewport=document.querySelector('#viewport');
const status=document.querySelector('#status');
const renderer=new THREE.WebGLRenderer({antialias:true,alpha:false,preserveDrawingBuffer:true});
renderer.setPixelRatio(Math.min(devicePixelRatio,2));
renderer.outputColorSpace=THREE.SRGBColorSpace;
renderer.toneMapping=THREE.NoToneMapping;
viewport.append(renderer.domElement);
const scene=new THREE.Scene();scene.background=new THREE.Color('#202820');
const camera=new THREE.OrthographicCamera(-1.3,1.3,1.3,-1.3,.01,100);
const loader=new GLTFLoader();
let profile,bug,left,right,antLeft,antRight;
let mode='front',playing=false,elapsed=0,lastTime=0,opening=13;
window.reviewState={loaded:false,frames:0,mode};

function resize(){const w=viewport.clientWidth,h=viewport.clientHeight;renderer.setSize(w,h,false)}
new ResizeObserver(resize).observe(viewport);
function selectMode(value){
  mode=value;window.reviewState.mode=mode;
  if(mode==='volume'){playing=true;document.querySelector('#play').textContent='暂停动作';}
  for(const id of ['front','volume','scale'])document.getElementById(id).classList.toggle('selected',id===value);
  updateCamera();
}
function updateCamera(){
  const extent=mode==='scale'?4.58:1.30;
  camera.left=-extent;camera.right=extent;camera.top=extent;camera.bottom=-extent;
  if(mode==='volume'){
    const a=elapsed*.55;
    camera.up.set(0,1,0);camera.position.set(Math.sin(a)*3,3.2,Math.cos(a)*3);
  }else{camera.up.set(0,0,-1);camera.position.set(0,5,.00001)}
  camera.lookAt(0,.08,0);camera.updateProjectionMatrix();
}
for(const id of ['front','volume','scale'])document.getElementById(id).onclick=()=>selectMode(id);
document.querySelector('#play').onclick=()=>{
  playing=!playing;document.querySelector('#play').textContent=playing?'暂停动作':'播放动作';
};
document.querySelector('#opening').oninput=e=>{
  playing=false;document.querySelector('#play').textContent='播放动作';opening=Number(e.target.value);
};
function frame(now){
  const dt=lastTime?Math.min((now-lastTime)/1000,.05):0;lastTime=now;
  if(playing)elapsed+=dt;
  if(bug){
    if(playing)opening=profile.shell_open_degrees+Math.sin(elapsed*2*Math.PI/profile.shell_period_seconds)*profile.shell_swing_degrees;
    left.rotation.y=-THREE.MathUtils.degToRad(opening);right.rotation.y=THREE.MathUtils.degToRad(opening);
    antLeft.rotation.y=playing?Math.sin(elapsed*2.1)*.025:0;
    antRight.rotation.y=playing?Math.sin(elapsed*2.1+.8)*.025:0;
    document.querySelector('#opening').value=opening;
    document.querySelector('#angle').value=opening.toFixed(1)+'°';
    updateCamera();renderer.render(scene,camera);window.reviewState.frames++;
  }
  requestAnimationFrame(frame);
}
try{
  profile=await fetch('/app/src/main/assets/profiles/reference_ladybug.json').then(r=>{if(!r.ok)throw Error(r.status);return r.json()});
  const [model,stage]=await Promise.all([
    loader.loadAsync('/app/src/main/assets/models/reference_ladybug.glb'),
    loader.loadAsync('/app/src/main/assets/models/reference_stage.glb')]);
  bug=model.scene;
  left=bug.getObjectByName('shell_left_hinge');right=bug.getObjectByName('shell_right_hinge');
  antLeft=bug.getObjectByName('antenna_left_pivot');antRight=bug.getObjectByName('antenna_right_pivot');
  if(!left||!right||!antLeft||!antRight)throw Error('模型部件不完整');
  scene.add(stage.scene,bug);updateCamera();resize();
  const current=bug;
  // The approved baseline is local review evidence; absence must not block a fresh clone.
  loader.loadAsync('/.local/reviews/p1/approved-r1/reference_ladybug.glb').then(previous=>{
    previous.scene.visible=false;scene.add(previous.scene);
    const compare=document.querySelector('#compare');compare.disabled=false;
    compare.onclick=()=>{
      const isPrevious=bug===current;
      current.visible=!isPrevious;previous.scene.visible=isPrevious;
      bug=isPrevious?previous.scene:current;
      left=bug.getObjectByName('shell_left_hinge');right=bug.getObjectByName('shell_right_hinge');
      antLeft=bug.getObjectByName('antenna_left_pivot');antRight=bug.getObjectByName('antenna_right_pivot');
      compare.textContent=isPrevious?'返回本轮':'查看上一版';
      status.textContent=isPrevious?'上一版 · 已认可':'本轮 · 非对称与明暗';
    };
  }).catch(()=>{document.querySelector('#compare').hidden=true;});
  window.reviewState.loaded=true;
  status.textContent='本轮 · 非对称与明暗';
  const params=new URLSearchParams(location.search);
  if(params.has('view'))selectMode(params.get('view'));
  if(params.has('time'))elapsed=Number(params.get('time'));
  requestAnimationFrame(frame);
}catch(error){status.textContent='加载失败：'+error.message;window.reviewState.error=error.message;console.error(error)}

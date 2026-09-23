// Speed owns the pose: navigation, body, wings and flexible antennae share one fixed-step clock.
const STEP=1/120,TAU=Math.PI*2;
const clamp=(x,a=0,b=1)=>Math.max(a,Math.min(b,x));
const mix=(a,b,t)=>a+(b-a)*t;
const smooth=x=>{x=clamp(x);return x*x*(3-2*x)};
const wrap=x=>Math.atan2(Math.sin(x),Math.cos(x));
const decay=(dt,tau)=>1-Math.exp(-dt/tau);
function stroke(phase,open,hold){
  const p=phase-Math.floor(phase),end=open+hold;
  if(p<open)return smooth(p/open)*2-1;
  if(p<end)return 1;
  return 1-smooth((p-end)/(1-end))*2;
}

export class BugMotion {
  constructor(config,rest=13){
    this.config=config;this.randomState=config.seed;this.selected='auto';this.active=0;
    this.time=0;this.age=0;this.accumulator=0;this.phase=0;this.activation=0;this.rest=rest;
    this.heading=0;this.turnRate=0;this.acceleration=0;this.wide=0;this.wideTarget=0;
    this.goalX=1.7;this.goalZ=-2.2;
    this.antennaVelocity=Array(6).fill(0);
    this.antennaRootPhase=0;
    this.bodyStretch=0;this.bodyStretchVelocity=0;
    this.pose={x:0,z:0,height:.018,yaw:0,pitch:0,roll:0,speed:0,energy:0,
      scaleX:1,scaleY:1,scaleZ:1,
      wingLeft:rest,wingRight:rest,wingHz:config.wing_frequency[0],swayAmplitude:0,
      antennae:Array(6).fill(0),label:'缓行',activity:'cruise'};
    this.choose(0);
  }
  random(){this.randomState=this.randomState*48271%2147483647;return this.randomState/2147483647}
  range(pair){return mix(pair[0],pair[1],this.random())}
  choose(index){
    this.active=index;this.age=0;
    const state=this.config.states[index];
    this.duration=this.range(state.duration);this.targetSpeed=this.range(state.speed);
    this.wideTarget=state.id==='dash'&&this.random()<.42?1:0;
  }
  select(id){
    const index=this.config.states.findIndex(s=>s.id===id);
    if(id!=='auto'&&index<0)throw Error('Unknown locomotion: '+id);
    this.selected=id;this.choose(id==='auto'?0:index);
  }
  setRestOpening(degrees){
    this.rest=clamp(degrees,0,this.config.max_open_degrees);this.activation=0;
    this.pose.wingLeft=this.rest;this.pose.wingRight=this.rest;
  }
  chooseGoal(){
    const angle=this.random()*TAU,radius=.9+this.random()*1.7;
    this.goalX=Math.cos(angle)*radius;this.goalZ=Math.sin(angle)*radius;
    if(Math.hypot(this.goalX-this.pose.x,this.goalZ-this.pose.z)<1.1){this.goalX=-this.goalX;this.goalZ=-this.goalZ}
  }
  advance(seconds,onStep){
    if(!Number.isFinite(seconds)||seconds<0)throw Error('Invalid delta');
    this.accumulator+=Math.min(seconds,1);
    while(this.accumulator+1e-10>=STEP){this.tick();onStep?.(this.pose);this.accumulator-=STEP}
    return this.pose;
  }
  tick(){
    const c=this.config,p=this.pose;
    if(this.age>=this.duration){
      let next=this.active;
      if(this.selected==='auto'){
        const id=c.states[this.active].id;
        next=id==='cruise'?(this.random()<.78?1:2):0;
      }
      this.choose(next);
    }
    this.age+=STEP;this.time+=STEP;this.activation+=STEP;
    if(Math.hypot(this.goalX-p.x,this.goalZ-p.z)<.60)this.chooseGoal();
    const stoppingDistance=p.speed*.25+p.speed*p.speed/(2*c.braking);
    const predictedX=p.x-Math.sin(this.heading)*stoppingDistance;
    const predictedZ=p.z-Math.cos(this.heading)*stoppingDistance;
    const avoidEdge=Math.hypot(predictedX,predictedZ)>c.arena_radius-.45;
    const targetX=avoidEdge?0:this.goalX,targetZ=avoidEdge?0:this.goalZ;
    const wantedHeading=Math.atan2(-(targetX-p.x),-(targetZ-p.z));
    const headingError=wrap(wantedHeading-this.heading);
    const wantedTurn=clamp(headingError*3.0,-2.8,2.8);
    this.turnRate=mix(this.turnRate,wantedTurn,decay(STEP,.18));
    // Slow down for a turn before accelerating along the new heading.
    let speedGoal=this.targetSpeed*mix(1,.38,clamp((Math.abs(headingError)-.45)/1.5));
    if(avoidEdge)speedGoal=Math.min(speedGoal,.40);
    const wantedAcceleration=clamp((speedGoal-p.speed)*3.8,-c.braking,c.acceleration);
    this.acceleration=mix(this.acceleration,wantedAcceleration,decay(STEP,c.response_seconds));
    p.speed=clamp(p.speed+this.acceleration*STEP,0,c.max_speed);
    this.heading=wrap(this.heading+this.turnRate*STEP);
    p.x-=Math.sin(this.heading)*p.speed*STEP;p.z-=Math.cos(this.heading)*p.speed*STEP;
    const energy=clamp(p.speed/c.max_speed);
    p.energy=energy;
    p.activity=c.states[this.active].id;
    p.label=this.acceleration>.35?'加速':this.acceleration<-.35?'减速':c.states[this.active].label;
    this.wide=mix(this.wide,this.wideTarget,decay(STEP,.38));
    // Full-rate reference frames reveal ~0.1 s cycles in the fastest segment.
    // Keep slow crawling gentle, then ramp up rapidly as actual speed increases.
    const drive=smooth((energy-.12)/.68);
    p.wingHz=mix(c.wing_frequency[0]+energy*3,c.wing_frequency[1],drive);
    this.phase+=p.wingHz*STEP;
    const phase=this.phase;
    const folded=12-drive*3;
    const excursion=mix(1.1+energy*20,mix(c.wing_excursion_degrees[0],c.wing_excursion_degrees[1],this.wide),drive);
    const amplitude=excursion/2,center=folded+amplitude;
    const open=mix(.65,.30,energy),hold=mix(.11,.04,energy);
    const fade=smooth(this.activation/c.transition_seconds);
    p.wingLeft=mix(this.rest,center+amplitude*stroke(phase,open,hold),fade);
    p.wingRight=mix(this.rest,center+amplitude*.95*stroke(phase+.035*Math.sin(this.time*.87),open,hold),fade);
    const bodyPhase=phase*Math.PI;
    p.swayAmplitude=mix(c.body_sway_degrees[0],c.body_sway_degrees[1],energy);
    p.yaw=this.heading+Math.sin(bodyPhase)*p.swayAmplitude*Math.PI/180;
    p.roll=(Math.sin(bodyPhase+.6)*p.swayAmplitude*.40-this.turnRate*energy*.85)*Math.PI/180;
    p.pitch=(Math.sin(bodyPhase*2+.2)*energy*1.5-this.acceleration*.45)*Math.PI/180;
    p.height=.018+.032*energy+.005*energy*Math.sin(bodyPhase*2);
    // Small coupled squash/stretch, not uniform screen-size pumping. Preserve
    // volume and joint attachments; acceleration leaves a short elastic recoil.
    const elastic=c.body_elastic;
    const stretchTarget=clamp(this.acceleration/(this.acceleration>=0?c.acceleration:c.braking),-1,1)*elastic.acceleration_stretch;
    this.bodyStretchVelocity+=(elastic.stiffness*(stretchTarget-this.bodyStretch)-elastic.damping*this.bodyStretchVelocity)*STEP;
    this.bodyStretch+=this.bodyStretchVelocity*STEP;
    const pulse=mix(elastic.pulse[0],elastic.pulse[1],drive)*Math.sin(bodyPhase-.65);
    p.scaleX=1+pulse-this.bodyStretch*.55;
    p.scaleZ=1+this.bodyStretch+pulse*.35;
    p.scaleY=1/(p.scaleX*p.scaleZ);
    const antenna=mix(c.antenna_sway_degrees[0],c.antenna_sway_degrees[1],energy);
    // The soft basal stem cannot follow 10 Hz wingbeats. Give its bend a slower
    // speed-driven phase; renderers distribute the total bend along four sections.
    this.antennaRootPhase+=mix(c.antenna_root.frequency[0],c.antenna_root.frequency[1],energy)*TAU*STEP;
    const rootPhase=this.antennaRootPhase,rootSway=mix(c.antenna_root.sway_degrees[0],c.antenna_root.sway_degrees[1],energy);
    const turnLag=-this.turnRate*2.0*(.08+energy);
    const force=this.acceleration;
    const targets=[
      Math.sin(rootPhase+.3)*rootSway+turnLag*.65-force*.85,
      Math.sin(bodyPhase-.4)*antenna*.95+turnLag*.75-force*2.2,
      Math.sin(bodyPhase-.9)*antenna*1.30+turnLag-force*3.4,
      Math.sin(rootPhase+1.15)*rootSway*.88+turnLag*.60+force*.80,
      Math.sin(bodyPhase+.45)*antenna*.86+turnLag*.75+force*2.0,
      Math.sin(bodyPhase-.1)*antenna*1.18+turnLag+force*3.1];
    for(let i=0;i<6;i++){
      const part=i%3,k=c.antenna_spring.stiffness[part],damping=c.antenna_spring.damping[part];
      this.antennaVelocity[i]+=(k*(targets[i]-p.antennae[i])-damping*this.antennaVelocity[i])*STEP;
      p.antennae[i]+=this.antennaVelocity[i]*STEP;
    }
  }
}

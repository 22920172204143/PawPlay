// Keep the fixed-step contract in sync with WingMotion.kt. A seed belongs to one insect.
const STEP=1/120;
const clamp=(x,a=0,b=1)=>Math.max(a,Math.min(b,x));
const smooth=x=>{x=clamp(x);return x*x*(3-2*x)};
const mix=(a,b,t)=>a+(b-a)*t;
const FREQ=0,CENTER=1,AMP=2,OPEN=3,HOLD=4,ASYM=5;

function stroke(phase,open,hold){
  const p=phase-Math.floor(phase),end=open+hold;
  if(p<open)return smooth(p/open)*2-1;
  if(p<end)return 1;
  return 1-smooth((p-end)/(1-end))*2;
}

export class WingMotion {
  constructor(config,rest=13){
    this.config=config;this.randomState=config.seed;this.mode='auto';
    this.active=-1;this.age=0;this.phase=0;this.time=0;this.accumulator=0;
    this.values=[1,rest,0,.5,.08,0];this.from=[...this.values];this.to=[...this.values];
    this.pose={left:rest,right:rest,antennaLeft:0,antennaRight:0,label:''};
    this.choose(0);
  }
  random(){this.randomState=(this.randomState*48271)%2147483647;return this.randomState/2147483647}
  range(pair){return mix(pair[0],pair[1],this.random())}
  choose(index){
    this.active=index;this.age=0;this.from=[...this.values];
    const m=this.config.modes[index];
    this.duration=this.range(m.duration);this.startFrequency=this.range(m.frequency);
    this.endFrequency=this.range(m.frequency);
    this.to=[this.startFrequency,this.range(m.center),this.range(m.amplitude),m.open_fraction,m.crest_hold,m.asymmetry];
    this.pose.label=m.label;
  }
  select(id){
    const index=this.config.modes.findIndex(m=>m.id===id);
    if(id!=='auto'&&index<0)throw Error('Unknown motion: '+id);
    this.mode=id;this.choose(id==='auto'?0:index);
  }
  setRestOpening(degrees){
    const opening=clamp(degrees,0,this.config.max_open_degrees);
    this.values=[1,opening,0,.5,.08,0];this.phase=0;this.accumulator=0;
    Object.assign(this.pose,{left:opening,right:opening,antennaLeft:0,antennaRight:0});
    this.choose(this.mode==='auto'?0:this.config.modes.findIndex(m=>m.id===this.mode));
  }
  advance(seconds){
    if(!Number.isFinite(seconds)||seconds<0)throw Error('Invalid delta');
    this.accumulator+=Math.min(seconds,1);
    while(this.accumulator+1e-10>=STEP){this.tick();this.accumulator-=STEP}
    return this.pose;
  }
  tick(){
    if(this.age>=this.duration){
      let next=this.active;
      if(this.mode==='auto'){
        // Pick without replacement against the current mode; duration and beat rate vary too.
        next=Math.floor(this.random()*(this.config.modes.length-1));
        if(next>=this.active)next++;
      }
      this.choose(next);
    }
    this.age+=STEP;this.time+=STEP;
    this.to[FREQ]=mix(this.startFrequency,this.endFrequency,smooth(this.age/this.duration));
    const blend=smooth(this.age/this.config.transition_seconds);
    for(let i=0;i<6;i++)this.values[i]=mix(this.from[i],this.to[i],blend);
    const v=this.values;
    this.phase=(this.phase+v[FREQ]*STEP)%1;
    const amplitude=v[AMP]*(.95+.05*Math.sin(this.time*1.73));
    const skew=Math.sin(this.time*.87)*v[ASYM];
    this.pose.left=clamp(v[CENTER]+amplitude*stroke(this.phase,v[OPEN],v[HOLD]),0,this.config.max_open_degrees);
    this.pose.right=clamp(v[CENTER]+amplitude*(.95+.05*Math.sin(this.time*.61))*stroke(this.phase+skew,v[OPEN],v[HOLD]),0,this.config.max_open_degrees);
    const antennaStrength=clamp(amplitude/2);
    this.pose.antennaLeft=Math.sin(this.time*2.1)*1.7*antennaStrength;
    this.pose.antennaRight=Math.sin(this.time*1.7+.8)*1.2*antennaStrength;
  }
}

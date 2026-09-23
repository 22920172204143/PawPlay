// World-space particles advance on BugMotion's 120 Hz clock. Independent RNG
// ensures choosing a different visual effect cannot change the insect's route.
const STEP=1/120,TAU=Math.PI*2;
const clamp=x=>Math.max(0,Math.min(1,x));
const mix=(a,b,t)=>a+(b-a)*t;
const smooth=value=>{const x=clamp(value);return x*x*(3-2*x)};
const linear=v=>{v/=255;return v<=.04045?v/12.92:((v+.055)/1.055)**2.4};

export class BugTrail {
  constructor(config){
    this.config=config;this.style=config.style;this.randomState=config.seed;
    this.particles=Array.from({length:config.max_particles},()=>({active:false,alpha:0}));
    this.credit=0;this.nextEmission=1;this.spawned=0;
    this.colors=Object.fromEntries(Object.entries(config.styles).map(([id,s])=>[id,s.palette_srgb.map(c=>c.map(linear))]));
  }
  random(){this.randomState=this.randomState*48271%2147483647;return this.randomState/2147483647}
  range(pair){return mix(pair[0],pair[1],this.random())}
  select(style){
    if(style!=='none'&&!this.config.styles[style])throw Error('Unknown trail: '+style);
    this.style=style;this.credit=0;this.nextEmission=1;
    this.particles.forEach(p=>{p.active=false;p.alpha=0});
  }
  step(pose){
    for(const p of this.particles){
      if(!p.active)continue;
      p.age+=STEP;
      if(p.age>=p.life){p.active=false;p.alpha=0;continue}
      this.update(p);
    }
    if(this.style==='none')return;
    const c=this.config,speed=pose.speed;
    const gate=smooth((speed-c.min_speed)/(c.full_emission_speed-c.min_speed));
    if(gate===0){this.credit=0;return}
    this.credit+=mix(c.rate[0],c.rate[1],clamp(speed/c.max_speed))*(c.styles[this.style].rate_multiplier??1)*gate*STEP;
    if(this.credit>=this.nextEmission){
      this.credit-=this.nextEmission;this.spawn(pose);
      this.nextEmission=.78+this.random()*.44;
    }
  }
  spawn(pose){
    const p=this.particles.find(p=>!p.active);if(!p)return;
    const c=this.config,s=c.styles[this.style],a=pose.yaw;
    const side=(this.random()*2-1)*c.spread;
    const behind=c.tail_offset*pose.scaleZ;
    p.style=this.style;p.age=0;p.life=this.range(s.lifetime);p.baseSize=this.range(s.size);
    p.startAngle=this.random()*TAU;p.spin=this.range(s.spin);p.opacity=this.range(s.opacity);
    p.color=this.colors[this.style][Math.floor(this.random()*this.colors[this.style].length)];
    p.ox=pose.x+Math.sin(a)*behind+Math.cos(a)*side;
    p.oz=pose.z+Math.cos(a)*behind-Math.sin(a)*side;p.oy=pose.height+.055;
    const drift=(this.random()*2-1)*s.drift;
    p.vx=-Math.sin(a)*pose.speed*s.inherit_velocity+Math.cos(a)*drift;
    p.vz=-Math.cos(a)*pose.speed*s.inherit_velocity-Math.sin(a)*drift;
    p.rise=s.rise;p.endScale=s.end_scale;p.active=true;p.id=++this.spawned;this.update(p);
  }
  update(p){
    const t=p.age/p.life;
    p.x=p.ox+p.vx*p.age;p.z=p.oz+p.vz*p.age;p.y=p.oy+p.rise*p.age;
    p.angle=p.startAngle+p.spin*p.age;
    p.size=p.baseSize*mix(.76,1,smooth(t/.14))*mix(1,p.endScale,smooth(t));
    p.alpha=p.opacity*smooth(p.age/.025)*(1-smooth((t-.18)/.82));
  }
}

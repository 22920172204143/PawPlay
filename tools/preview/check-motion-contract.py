"""Compare the compiled Android motion controller with the browser over the same input sequence.

Run after assembleDebug. Requires Node, JDK 17 and a Kotlin stdlib jar from the Gradle cache.
Only temporary evidence under .local/ is written; Android/graphics initialization is not used.
"""
import argparse
import json
import os
from pathlib import Path
import subprocess


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--java-home', type=Path, required=True)
    parser.add_argument('--stdlib', type=Path, required=True)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[2]
    out = root/'.local/reviews/p2/motion-contract'
    out.mkdir(parents=True,exist_ok=True)
    profile=json.loads((root/'art/insects/reference_ladybug/model.json').read_text(encoding='utf-8'))
    cfg=profile['locomotion'];trail=profile['trail']
    javascript=f"import {{BugMotion}} from '{(root/'tools/preview/bug-motion.mjs').as_uri()}';\n"
    javascript+=f"import {{BugTrail}} from '{(root/'tools/preview/bug-trail.mjs').as_uri()}';\n"
    javascript+="import {writeFileSync} from 'node:fs';\n"
    javascript+=f"const motion=new BugMotion({json.dumps(cfg)});\n"
    javascript+=f"const trail=new BugTrail({json.dumps(trail)});\n"
    javascript+='''let rows=[],trailRows=[];
for(let i=0;i<4800;i++){
 const action=({600:'dash',1200:'cruise',1800:'pause',2400:'auto',3000:'rest',3060:'dash',3600:'auto'})[i]||'-';
 if(action==='rest')motion.setRestOpening(38);else if(action!=='-')motion.select(action);
 const style=({450:'bubbles',1500:'none',1800:'stars',2400:'bubbles',3600:'stars'})[i];
 if(style)trail.select(style);
 const dt=i>=3000&&i<3060?0:1/60,p=motion.advance(dt,p=>trail.step(p));
 rows.push([i,dt,action,p.x,p.z,p.speed,p.height,p.yaw,p.pitch,p.roll,p.wingLeft,p.wingRight,p.wingHz,p.swayAmplitude,...p.antennae,p.scaleX,p.scaleY,p.scaleZ].join('\\t'));
 if(i%6===0){
  const active=trail.particles.flatMap((p,slot)=>p.active?[[slot,p.id,p.style==='bubbles'?1:0,p.age,p.life,p.x,p.y,p.z,p.angle,p.size,p.alpha,...p.color]]:[]);
  trailRows.push([i,trail.spawned,active.length,...active.flat()].join('\\t'));
 }
}
writeFileSync(new URL('./samples.tsv',import.meta.url),rows.join('\\n'));
writeFileSync(new URL('./trail-samples.tsv',import.meta.url),trailRows.join('\\n'));
'''
    (out/'reference.mjs').write_text(javascript,encoding='utf-8')
    subprocess.run(['node',str(out/'reference.mjs')],check=True)
    def number(value): return str(float(value))
    def pair(values): return 'new kotlin.Pair<Double,Double>('+','.join(map(number,values))+')'
    states=[]
    for state in cfg['states']:
        states.append('new BugMotion.State('+','.join([json.dumps(state['id']),json.dumps(state['label']),
                                                      pair(state['speed']),pair(state['duration'])])+')')
    arguments=[str(cfg['seed'])+'L']+[number(cfg[key]) for key in ['arena_radius','max_speed','acceleration',
               'braking','response_seconds','transition_seconds','max_open_degrees']]
    arguments+=['List.of('+','.join(states)+')']+[pair(cfg[key]) for key in ['wing_frequency','body_sway_degrees','antenna_sway_degrees']]
    arguments+=['new double[]{'+','.join(map(number,cfg['antenna_spring'][key]))+'}' for key in ['stiffness','damping']]
    elastic=cfg['body_elastic']
    arguments+=[pair(cfg['wing_excursion_degrees']), 'new BugMotion.Elastic('+','.join([pair(elastic['pulse'])]+
                [number(elastic[key]) for key in ['acceleration_stretch','stiffness','damping']])+')']
    arguments+=[pair(cfg['antenna_root'][key]) for key in ['frequency','sway_degrees']]
    trail_styles=[]
    for name,s in trail['styles'].items():
        style_args=[pair(s[key]) for key in ['lifetime','size','spin','opacity']]
        style_args += [number(s[key]) for key in ['drift','rise','inherit_velocity','end_scale']]
        style_args += ['List.of('+','.join('new double[]{'+','.join(map(number,c))+'}' for c in s['palette_srgb'])+')']
        style_args += [number(s.get('rate_multiplier',1))]
        trail_styles += [json.dumps(name), 'new BugTrail.Style('+','.join(style_args)+')']
    trail_args=[json.dumps(trail['style']),str(trail['seed'])+'L',str(trail['max_particles'])]
    trail_args += [number(trail[key]) for key in ['tail_offset','spread','min_speed','full_emission_speed','max_speed']]
    trail_args += [pair(trail['rate']), 'Map.of('+','.join(trail_styles)+')']
    java='''import com.pawhunt.app.preview.BugMotion;
import com.pawhunt.app.preview.BugTrail;
import java.nio.file.*;
import java.util.*;
public class MotionContract {
 public static void main(String[] args) throws Exception {
'''
    java+='BugMotion motion=new BugMotion(new BugMotion.Config('+','.join(arguments)+'),13.0);\n'
    java+='BugTrail trail=new BugTrail(new BugTrail.Config('+','.join(trail_args)+'));\n'
    java+='''double maxError=0,trailError=0;int frames=0,trailSamples=0;
 List<String> trailRows=Files.readAllLines(Path.of(args[1]));
 for(String row:Files.readAllLines(Path.of(args[0]))){
  String[] col=row.split("\\t");String action=col[2];
  if(action.equals("rest"))motion.setRestOpening(38);else if(!action.equals("-"))motion.select(action);
  String style=switch(frames){case 450,2400 -> "bubbles";case 1500 -> "none";case 1800,3600 -> "stars";default -> null;};
  if(style!=null)trail.select(style);
  BugMotion.Pose p=motion.advance(Double.parseDouble(col[1]),step->{trail.step(step);return kotlin.Unit.INSTANCE;});
  double[] got={p.getX(),p.getZ(),p.getSpeed(),p.getHeight(),p.getYaw(),p.getPitch(),p.getRoll(),
    p.getWingLeft(),p.getWingRight(),p.getWingHz(),p.getSwayAmplitude(),p.getAntennae()[0],p.getAntennae()[1],
    p.getAntennae()[2],p.getAntennae()[3],p.getAntennae()[4],p.getAntennae()[5],p.getScaleX(),p.getScaleY(),p.getScaleZ()};
  for(int i=0;i<got.length;i++)maxError=Math.max(maxError,Math.abs(got[i]-Double.parseDouble(col[i+3])));
  if(frames%6==0){
   List<Double> values=new ArrayList<>();values.add((double)frames);values.add((double)trail.getSpawned());
   values.add((double)trail.getParticles().stream().filter(BugTrail.Particle::getActive).count());
   for(int slot=0;slot<trail.getParticles().size();slot++){
    BugTrail.Particle q=trail.getParticles().get(slot);if(!q.getActive())continue;
    double[] particle={slot,q.getId(),q.getStyle().equals("bubbles")?1:0,q.getAge(),q.getLife(),q.getX(),q.getY(),q.getZ(),q.getAngle(),q.getSize(),q.getAlpha(),q.getColor()[0],q.getColor()[1],q.getColor()[2]};
    for(double value:particle)values.add(value);
   }
   String[] expected=trailRows.get(trailSamples++).split("\\t");
   if(values.size()!=expected.length)throw new AssertionError("Trail population differs at frame "+frames);
   for(int i=0;i<values.size();i++)trailError=Math.max(trailError,Math.abs(values.get(i)-Double.parseDouble(expected[i])));
  }
  frames++;
 }
 if(maxError>1e-8)throw new AssertionError("Cross-runtime mismatch: "+maxError);
 if(trailError>1e-8)throw new AssertionError("Cross-runtime trail mismatch: "+trailError);
 System.out.println("Android JVM / browser locomotion contract passed: "+frames+" frames, max error "+maxError);
 System.out.println("Android JVM / browser trail contract passed: "+trailSamples+" samples, max error "+trailError);
 }
}
'''
    (out/'MotionContract.java').write_text(java,encoding='utf-8')
    classpath=os.pathsep.join(map(str,[root/'app/build/tmp/kotlin-classes/debug',args.stdlib.resolve(),out]))
    suffix='.exe' if os.name=='nt' else ''
    subprocess.run([str(args.java_home/'bin'/('javac'+suffix)),'-encoding','UTF-8','-cp',classpath,str(out/'MotionContract.java')],check=True)
    result=subprocess.run([str(args.java_home/'bin'/('java'+suffix)),'-cp',classpath,'MotionContract',str(out/'samples.tsv'),str(out/'trail-samples.tsv')],
                          check=True,capture_output=True,text=True)
    (out/'result.txt').write_text(result.stdout,encoding='utf-8')
    print(result.stdout)


if __name__=='__main__':main()

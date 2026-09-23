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
    cfg=json.loads((root/'art/insects/reference_ladybug/model.json').read_text(encoding='utf-8'))['locomotion']
    javascript=f"import {{BugMotion}} from '{(root/'tools/preview/bug-motion.mjs').as_uri()}';\n"
    javascript+="import {writeFileSync} from 'node:fs';\n"
    javascript+=f"const motion=new BugMotion({json.dumps(cfg)});\n"
    javascript+='''let rows=[];
for(let i=0;i<4800;i++){
 const action=({600:'dash',1200:'cruise',1800:'pause',2400:'auto',3000:'rest',3060:'dash',3600:'auto'})[i]||'-';
 if(action==='rest')motion.setRestOpening(38);else if(action!=='-')motion.select(action);
 const dt=i>=3000&&i<3060?0:1/60,p=motion.advance(dt);
 rows.push([i,dt,action,p.x,p.z,p.speed,p.height,p.yaw,p.pitch,p.roll,p.wingLeft,p.wingRight,p.wingHz,p.swayAmplitude,...p.antennae,p.scaleX,p.scaleY,p.scaleZ].join('\\t'));
}
writeFileSync(new URL('./samples.tsv',import.meta.url),rows.join('\\n'));
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
    java='''import com.pawhunt.app.preview.BugMotion;
import java.nio.file.*;
import java.util.*;
public class MotionContract {
 public static void main(String[] args) throws Exception {
'''
    java+='BugMotion motion=new BugMotion(new BugMotion.Config('+','.join(arguments)+'),13.0);\n'
    java+='''double maxError=0;int frames=0;
 for(String row:Files.readAllLines(Path.of(args[0]))){
  String[] col=row.split("\\t");String action=col[2];
  if(action.equals("rest"))motion.setRestOpening(38);else if(!action.equals("-"))motion.select(action);
  BugMotion.Pose p=motion.advance(Double.parseDouble(col[1]));
  double[] got={p.getX(),p.getZ(),p.getSpeed(),p.getHeight(),p.getYaw(),p.getPitch(),p.getRoll(),
    p.getWingLeft(),p.getWingRight(),p.getWingHz(),p.getSwayAmplitude(),p.getAntennae()[0],p.getAntennae()[1],
    p.getAntennae()[2],p.getAntennae()[3],p.getAntennae()[4],p.getAntennae()[5],p.getScaleX(),p.getScaleY(),p.getScaleZ()};
  for(int i=0;i<got.length;i++)maxError=Math.max(maxError,Math.abs(got[i]-Double.parseDouble(col[i+3])));
  frames++;
 }
 if(maxError>1e-8)throw new AssertionError("Cross-runtime mismatch: "+maxError);
 System.out.println("Android JVM / browser locomotion contract passed: "+frames+" frames, max error "+maxError);
 }
}
'''
    (out/'MotionContract.java').write_text(java,encoding='utf-8')
    classpath=os.pathsep.join(map(str,[root/'app/build/tmp/kotlin-classes/debug',args.stdlib.resolve(),out]))
    suffix='.exe' if os.name=='nt' else ''
    subprocess.run([str(args.java_home/'bin'/('javac'+suffix)),'-encoding','UTF-8','-cp',classpath,str(out/'MotionContract.java')],check=True)
    result=subprocess.run([str(args.java_home/'bin'/('java'+suffix)),'-cp',classpath,'MotionContract',str(out/'samples.tsv')],
                          check=True,capture_output=True,text=True)
    (out/'result.txt').write_text(result.stdout,encoding='utf-8')
    print(result.stdout)


if __name__=='__main__':main()

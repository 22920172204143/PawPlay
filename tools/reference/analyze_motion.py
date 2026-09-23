"""Track the local reference at 10 fps; estimates are evidence, not animation ground truth."""
import argparse
import json
from pathlib import Path
import subprocess
import numpy as np
from PIL import Image
from extract_reference import red_shell_bounds


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--video', required=True)
    parser.add_argument('--ffmpeg', required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    process = subprocess.Popen([args.ffmpeg,'-v','error','-i',args.video,'-t','30',
                                '-vf','fps=10','-f','rawvideo','-pix_fmt','rgb24','-'],stdout=subprocess.PIPE)
    records = []
    for i in range(300):
        raw = process.stdout.read(1280*720*3)
        if len(raw) != 1280*720*3:
            break
        if not 7 <= i <= 291:
            continue
        frame = Image.frombytes('RGB',(1280,720),raw)
        bounds = red_shell_bounds(frame)
        if not bounds:
            continue
        x0,y0,x1,y1 = bounds
        cx,cy = (x0+x1)/2,(y0+y1)/2
        records.append({'time':i/10,'x':cx,'y':cy,'red_width':x1-x0,'red_height':y1-y0})
    if process.wait() != 0:
        raise RuntimeError('Video decoding failed')
    positions = np.array([[r['x'],r['y']] for r in records])
    # A centered 0.4 s displacement reduces center shifts caused by wings and compression.
    for i in range(2,len(records)-2):
        span = records[i+2]['time']-records[i-2]['time']
        records[i]['speed_px_s'] = float(np.linalg.norm(positions[i+2]-positions[i-2])/span)
    windows=[]
    for start in range(1,28,2):
        subset=[r for r in records if start<=r['time']<start+2 and 'speed_px_s' in r]
        windows.append({'seconds':[start,start+2],
                        'median_speed_px_s':round(float(np.median([r['speed_px_s'] for r in subset])),1),
                        'samples':len(subset)})
    args.output.mkdir(parents=True,exist_ok=True)
    (args.output/'tracking.json').write_text(json.dumps({'windows':windows,'frames':records},indent=2))
    print(json.dumps(windows,indent=2))


if __name__=='__main__':main()

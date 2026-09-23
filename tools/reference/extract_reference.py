"""Extract reproducible local reference frames; never modify the source video.

Requires Python, Pillow, NumPy and FFmpeg. Outputs are intended for .local/.
Color-based bounds describe the red shell only, not the full insect silhouette.
"""

import argparse
import hashlib
import json
import math
from pathlib import Path
import re
import subprocess

import numpy as np
from PIL import Image, ImageDraw


def red_shell_bounds(image):
    rgb = np.asarray(image.convert("RGB"), dtype=np.int16)
    red, green, blue = rgb[:, :, 0], rgb[:, :, 1], rgb[:, :, 2]
    mask = (red > 130) & (red > green * 1.7) & (red > blue * 1.4)
    height, width = mask.shape
    # This reference has side bars, an upper watermark and a lower video overlay.
    mask[: round(height * 0.139), :] = False
    mask[round(height * 0.903) :, :] = False
    mask[:, : round(width * 0.133)] = False
    mask[:, round(width * 0.875) :] = False
    ys, xs = np.where(mask)
    if len(xs) < 100:
        return None
    center_x, center_y = int(np.median(xs)), int(np.median(ys))
    # Reject distant red overlay pixels without pretending to segment every part.
    local = (abs(xs - center_x) < 110) & (abs(ys - center_y) < 110)
    xs, ys = xs[local], ys[local]
    return [int(xs.min()), int(ys.min()), int(xs.max()) + 1, int(ys.max()) + 1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--video", type=Path, required=True)
    parser.add_argument("--ffmpeg", required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--times", nargs="+", type=float, default=list(range(1, 30, 2)))
    args = parser.parse_args()
    video = args.video.resolve(strict=True)
    output = args.output.resolve()
    if any(not math.isfinite(t) or t < 0 for t in args.times):
        parser.error("Frame times must be finite non-negative seconds.")
    output.mkdir(parents=True, exist_ok=True)
    (output / "frames").mkdir(exist_ok=True)
    # A metadata-only ffmpeg invocation exits nonzero because no output is supplied.
    probe = subprocess.run(
        [args.ffmpeg, "-hide_banner", "-i", str(video)],
        capture_output=True, text=True, encoding="utf-8", errors="replace",
    )
    (output / "video-metadata.txt").write_text(probe.stderr, encoding="utf-8")
    duration_match = re.search(r"Duration: (\d+):(\d+):(\d+\.\d+)", probe.stderr)
    if not duration_match:
        raise RuntimeError("Could not read video duration; inspect video-metadata.txt.")
    hours, minutes, seconds = map(float, duration_match.groups())
    duration = hours * 3600 + minutes * 60 + seconds
    if any(t >= duration for t in args.times):
        parser.error(f"All sample times must be below {duration:.2f} seconds.")

    cols = 5
    rows = math.ceil(len(args.times) / cols)
    overview = Image.new("RGB", (cols * 400, rows * 250), "#141c28")
    details = Image.new("RGB", (cols * 280, rows * 308), "#141c28")
    overview_labels, detail_labels = ImageDraw.Draw(overview), ImageDraw.Draw(details)
    records = []
    for index, time_seconds in enumerate(args.times):
        name = f"frame-{index:02d}-{time_seconds:06.2f}s.png"
        destination = output / "frames" / name
        subprocess.run(
            [args.ffmpeg, "-nostdin", "-hide_banner", "-loglevel", "error", "-y",
             "-ss", str(time_seconds), "-i", str(video), "-frames:v", "1",
             str(destination)],
            check=True,
        )
        with Image.open(destination) as raw:
            frame = raw.convert("RGB")
        bounds = red_shell_bounds(frame)
        col, row = index % cols, index // cols
        overview.paste(frame.resize((400, 225)), (col * 400, row * 250))
        overview_labels.text((col * 400 + 8, row * 250 + 231),
                             f"seek {time_seconds:.2f}s", fill="white")
        if bounds:
            left, top, right, bottom = bounds
            cx, cy = (left + right) // 2, (top + bottom) // 2
            crop = frame.crop((cx - 100, cy - 100, cx + 100, cy + 100))
            details.paste(crop.resize((280, 280)), (col * 280, row * 308))
        detail_labels.text((col * 280 + 8, row * 308 + 286),
                           f"seek {time_seconds:.2f}s", fill="white")
        records.append({"requested_time_seconds": time_seconds,
                        "frame": f"frames/{name}",
                        "frame_size": list(frame.size),
                        "red_shell_bbox_xyxy": bounds})
    overview.save(output / "overview.jpg", quality=94)
    details.save(output / "bug-details.jpg", quality=95)
    with video.open("rb") as source:
        digest = hashlib.file_digest(source, "sha256").hexdigest()
    manifest = {
        "source": str(video), "source_sha256": digest,
        "source_bytes": video.stat().st_size, "duration_seconds": duration,
        "notes": [
            "Times are requested seek times; decoded frame timing can differ by one source frame.",
            "Bounds use a reference-specific red threshold, omit yellow parts and are approximate.",
            "Full frames retain original video overlays. Crops are analysis aids, not game assets.",
        ],
        "samples": records,
    }
    (output / "manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    widths = [r["red_shell_bbox_xyxy"][2] - r["red_shell_bbox_xyxy"][0]
              for r in records if r["red_shell_bbox_xyxy"]]
    heights = [r["red_shell_bbox_xyxy"][3] - r["red_shell_bbox_xyxy"][1]
               for r in records if r["red_shell_bbox_xyxy"]]
    print(json.dumps({"output": str(output), "sample_count": len(records),
                      "source_sha256": digest,
                      "red_shell_width_range_px": [min(widths), max(widths)] if widths else None,
                      "red_shell_height_range_px": [min(heights), max(heights)] if heights else None},
                     ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()

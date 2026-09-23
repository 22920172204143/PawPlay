# 本地参考抽帧

`extract_reference.py` 用 FFmpeg 抽取完整源帧，通过 Pillow 生成概览和局部图，并记录源文件哈希。红壳包围盒是针对当前视频的 NumPy 颜色阈值结果，不是通用分割算法或完整虫子遮罩。

依赖：Python 3.11+、Pillow、NumPy、FFmpeg。工具不修改原视频；输出使用 `.local/`，不提交视频、抽帧或机器路径。

```powershell
python tools/reference/extract_reference.py `
  --video 'C:\path\reference.mp4' `
  --ffmpeg 'C:\path\ffmpeg.exe' `
  --output '.local\reference\baseline'
```

默认抽取 1、3、5……29 秒。其他时间使用 `--times 5.0 5.1 5.2`。该默认范围针对本项目约 33 秒参考视频；更短视频必须指定有效时间。

输出包括 `frames/*.png`、`overview.jpg`、`bug-details.jpg`、`video-metadata.txt` 和 `manifest.json`。请求 seek 时间与实际源帧可能存在一个帧间隔内的偏差，不能把清单当成逐帧精确时间码。重复运行会更新同名输出；切换采样方案应使用新的输出目录。

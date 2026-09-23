# 本地模型预览

需要 Python 3.12 与 Node/npm。仓库根目录执行：

```powershell
npm install --prefix .local/web/vendor-package three@0.180.0
New-Item -ItemType Directory -Force .local/web/vendor
Copy-Item -Recurse .local/web/vendor-package/node_modules/three .local/web/vendor/three
python tools/preview/serve.py
```

打开 http://127.0.0.1:8765/tools/preview/index.html 。参考截图及视频属于本地证据，不入库；没有它们仍可查看模型。

模型重新生成：Blender 4.5.9 运行 `--background --python-exit-code 1 --python tools/art/build_reference_ladybug.py`。参数在 art/insects/reference_ladybug/model.json。导出坐标 +Y 向上、-Z 朝前。材质使用自制颜色贴图表现明暗，保留实际三维曲面。

Android：配置 JDK 17 与 Android SDK 34 后运行 `gradlew.bat :app:assembleDebug :app:lintDebug`。debug 是独立安装的瓢虫预览，release 保留原游戏入口。

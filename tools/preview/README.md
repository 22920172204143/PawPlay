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

动作参数与调节方式见 [翅壳动作分析](../../docs/wing-motion-analysis.md)。浏览器动作用 `node --test tools/preview/wing-motion.test.mjs` 检查。点击播放后默认自然变化，也可单独选择模式。

revision 4 可用“场景运动”看快慢路线、“跟随近看”看姿态联动。下拉框为自然巡游/慢速/快速/停留。检查控制器：

```powershell
node --test tools/preview/bug-motion.test.mjs tools/preview/wing-motion.test.mjs tools/preview/bug-trail.test.mjs
python tools/preview/check-motion-contract.py --java-home <JDK17路径> --stdlib <Gradle缓存中的kotlin-stdlib-1.9.22.jar>
```

跨运行时检查需先 assembleDebug，直接运行编译后的 Android 运动类，验证与浏览器在同一输入下的位置、速度、姿态和触角一致；它不代替手机渲染验收。

revision 5 加强触角中段/末梢的柔软度和回弹。选择“加速后停下”循环查看减速余摆；“查看调整前触角”沿用同一运动时刻，可暂停后比较。调整前控制器及配置保存在本地 `.local/reviews/p1/before-softness-r4/bug-motion.mjs` 和 `profile.json`，缺少它们会隐藏比较按钮，不影响当前模型。

当前 revision 6 可用“快速观察”对照大幅高速拍翅；“身体弹性”开关只改变形变显示，不重置运动。Android 预览也有此开关。当前对比基线更新为 `.local/reviews/p1/before-flight-r5/`；基线未获用户认可，按钮只表示调整前后。

左侧“原片片段”支持源帧率动态对照，本地短片在 `.local/reviews/p1/reference-r6/`，其 manifest 记录起始时刻、帧率、固定裁剪尺寸。分析帧与短片不入库，干净环境无这些证据仍可查看当前模型。

revision 7 将触角根部改为四段渐进弯曲。点击“触角近看”固定头部朝向并放大根部；需要将该版 GLB 与新 profile 一起使用，Node 检查会校验导出的四段根部层级。

revision 8 加大根部摆幅，并提高贴近头部部分的弯曲比例，已获用户认可。

revision 9 增加星星和泡泡拖尾。下拉菜单选择“模型默认 / 金色星星 / 透明泡泡 / 关闭”；“隐藏拖尾对比”只改变可见性，保留同一路线和时刻。选择“快速观察”看连续尾迹，“加速后停下”看减速后的消散。本轮无需本地旧版快照即可对比。

revision 10 将星星发射密度提高 50%、尺寸提高 40%；模型可用样式级 `rate_multiplier` 单独调节数量，不影响其他拖尾样式。

当前 revision 11 加强泡泡的彩虹反光、弧形高光和边缘可见度。选择“彩虹泡泡”查看；本轮需要同步最新 `insect_trail.glb` 和 profile。

拖尾资产重建：`python tools/art/build_trail_assets.py`，仅使用 Python 标准库。参数和按模型选择方式见 [拖尾效果](../../docs/trail-effects.md)。Android 预览也有同样的样式选择；跨运行时脚本现在同时比较运动和粒子生命周期，运行前需重新 assembleDebug。

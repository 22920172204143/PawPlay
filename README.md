# PawPlay - A Game for Your Cat

An Android app that entertains cats with interactive moving targets on screen.

## Features

- **10 toy types**: Laser dot, fish, cockroach, butterfly, mouse, spider, bee, feather, bird, yarn ball
- **6 backgrounds**: Grass, wood floor, water, night sky, city floor, pink blanket
- **Bezier curve motion**: Realistic prey movement with per-type behaviors
- **Hit detection**: Touch-based with particle effects, sound, and haptic feedback
- **Settings**: Speed control, prey count (1-3), sound/haptic toggles
- **Monetization**: Google Play Billing (Pro one-time + yearly subscription) + AdMob

## Tech Stack

- **Language**: Kotlin 1.9+
- **UI**: Jetpack Compose (Material 3)
- **Game Engine**: SurfaceView + Canvas + custom 60fps GameLoop
- **Billing**: Google Play Billing Library 6
- **Ads**: Google AdMob
- **Audio**: SoundPool
- **Haptics**: VibrationEffect
- **Persistence**: DataStore Preferences

## Build

1. Open in Android Studio (Hedgehog or newer)
2. Replace AdMob app ID in `AndroidManifest.xml` with your real ID
3. Replace billing product IDs in `BillingManager.kt`
4. Build and run on API 28+ device

## 给新 Agent / 干净环境的交接说明（最小可构建）

本仓库刻意**不提交编译产物**（例如 `app/build/`、`*.apk`、`*.aab`），`.gitignore` 已忽略它们。拉取后应只包含源码、Gradle 配置与 `res/` 资源等构建必需内容。

### 1) 必备环境

- **JDK 17**（与 Android Gradle Plugin 常见要求一致）
- **Android SDK**（含 Platform 与 Build-Tools；具体版本以 `app/build.gradle.kts` / `gradle/libs.versions.toml` 为准）

### 2) `local.properties`

Android Studio 会自动生成 `local.properties`（且已被 git 忽略）。在纯命令行环境需要手动创建 `PawPlay/local.properties`：

```properties
sdk.dir=/path/to/Android/Sdk
```

### 3) 命令行构建（Linux / macOS / WSL）

```bash
cd PawPlay
chmod +x gradlew
./gradlew :app:assembleDebug
```

Windows（PowerShell）可直接：

```powershell
cd PawPlay
.\gradlew.bat :app:assembleDebug
```

### 4) 音频资源位置

短音效与环境循环音放在：`app/src/main/res/raw/`

命名需与 `com.pawhunt.app.service.SoundManager` 中 `loadSoundSafe(...)` / `ambientResNames` 一致（小写 + 下划线，扩展名小写 `.mp3`）。

**重要**：`res/raw` 下**同一资源基名**只能有一种扩展名（例如不要同时存在 `hit.ogg` 与 `hit.mp3`），否则 `mergeDebugResources` 会报 `Duplicate resources`。

当前仓库内的 `*.mp3` 来自本机 PawHunt 工程中体积正常的 `assets/audio/lure/` 文件（`lure_bug_cricket`、`lure_bird_chirp`、`lure_fish_bubbles`、`lure_mouse_squeaks`），按玩法需要映射到 `sfx_hit_*` / `amb_*` / UI 等文件名；便于新环境拉取即可出声。若你要替换为自有素材：保持同名文件覆盖即可，并自行确认授权与体积（环境循环音会 `MediaPlayer` 循环播放，文件过长会增大 APK）。

## Min SDK

- Android 9.0 (API 28)
- Target: Android 14 (API 34)

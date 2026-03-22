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

## Min SDK

- Android 9.0 (API 28)
- Target: Android 14 (API 34)

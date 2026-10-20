# Play Console Quality & UI Verification (1.0.16 / versionCode 17)

This document records the root causes and verification of the 5 Google Play Console recommendations flagged on release `16 (1.0.15)` and the Mandir Sajaye action button layout fix.

## 1. Edge-to-Edge Backward Compatibility
- **Play Console finding**: *From Android 15, apps targeting SDK 35 will display edge-to-edge by default... Alternatively, call `enableEdgeToEdge()` for Kotlin or `EdgeToEdge.enable()` for Java for backward compatibility.*
- **Root cause in 1.0.15**: R8 inlined `androidx.activity.EdgeToEdge.enable` into `MainActivity.onCreate` and stripped the `androidx.activity.EdgeToEdge` class from `classes.dex`.
- **Fix in 1.0.16**:
  - `MainActivity.onCreate` calls `enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT), navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))` with `androidx.activity:activity-compose:1.11.0`.
  - `android/app/proguard-rules.pro` keeps `androidx.activity.EdgeToEdge**` and `androidx.activity.SystemBarStyle**` so `MainActivity.onCreate` retains the explicit `androidx.activity.EdgeToEdge.enable` invocation in `classes.dex`.
  - All screens, full-screen dialogs (`ShrineCustomizer`, `BhajanPlayerSheet`, `MandirNavigationDrawer`), and popups consume `WindowInsets.safeDrawing` (status bars, navigation bars, and display cutouts).

## 2. Deprecated Edge-to-Edge Window APIs
- **Play Console finding**: *Your app uses `android.view.Window.setStatusBarColor` and `android.view.Window.setNavigationBarColor` starting in `d.q.a` and `d.r.a`.*
- **Root cause in 1.0.15**: `d.q.a` and `d.r.a` were the R8-obfuscated names of `androidx.activity.EdgeToEdgeApi26.setUp` and `androidx.activity.EdgeToEdgeApi29.setUp`. Because R8 renamed those classes out of the `androidx.activity` package, Play Console's static bytecode analyzer did not recognize them as Jetpack's backward-compatibility implementation.
- **Fix in 1.0.16**:
  - `MainActivity` does not call `setStatusBarColor` or `setNavigationBarColor`.
  - `android/app/proguard-rules.pro` preserves `androidx.activity.EdgeToEdge**` class and method names so Jetpack's API 26/29 backward-compatibility classes remain in `androidx.activity.EdgeToEdge*` instead of being obfuscated into app-level short names (`d.q.a`, `d.r.a`).

## 3. Bitmap Image Optimization
- **Play Console finding**: *Your app is manually downloading and decoding images from the network: Decoded in `S0.a.c` (`BitmapFactory.decodeStream`), Downloaded in `A1.u.d`, `w3.l.invokeSuspend`, `y3.g.invokeSuspend` (`HttpURLConnection.getInputStream`).*
- **Root cause in 1.0.15**:
  - `BhajanPlayerView.kt` and `ShrineCustomizer.kt` decoded artwork thumbnails manually.
  - `S0.a.c` (`androidx.core.graphics.drawable.IconCompat$Api23Impl.createIcon`) and `A1.u.d` (`androidx.media3.datasource.DefaultHttpDataSource`) were obfuscated by R8, and `DevotionAccount.request` (`w3.l.invokeSuspend`) and `BhajanPlayer.fetchCatalog` (`y3.g.invokeSuspend`) used `HttpURLConnection.getInputStream`.
  - `MandirAltarView.kt`, `RitualGlassPanel.kt`, and `TempleDoors.kt` decoded local assets repeatedly with `BitmapFactory.decodeStream` without shared caching or downsampling.
- **Fix in 1.0.16**:
  - Added `io.coil-kt:coil-compose:2.7.0` for size-aware, cached thumbnail loading in `BhajanPlayerView.kt` and `ShrineCustomizer.kt`.
  - Added `AssetImageCache` (`LruCache`-backed `BitmapFactory.decodeByteArray` loader) for `MandirAltarView.kt`, `TempleDoors.kt`, and `RitualGlassPanel.kt` (downsampling the blurred dock backdrop by 4x). Zero `com.worship.nityamandir` classes call `BitmapFactory.decodeStream`.
  - Updated `DevotionAccount.kt` and `BhajanPlayer.kt` to use `OkHttpClient` instead of `HttpURLConnection.getInputStream`.
  - Added `-keepnames` rules for `androidx.core.**`, `androidx.media3.**`, `coil.**`, and `okhttp3.**` in `proguard-rules.pro`.

## 4. R8 Optimization & Android Gradle Plugin 9.0+
- **Play Console finding**: *Optimized resource shrinking isn't enabled; Upgrade your Android Gradle plugin to version 9.0 or higher.*
- **Root cause in 1.0.15**: The uploaded `1.0.15` bundle had been built prior to the AGP 9.0.1 toolchain commit.
- **Fix in 1.0.16**:
  - Built with Android Gradle Plugin `9.0.1`, Gradle `9.1.0`, and `android.r8.optimizedResourceShrinking=true` in `android/gradle.properties`.
  - Verified in `BUNDLE-METADATA/com.android.tools/r8.json` (`"isOptimizedShrinkingEnabled": true`) and `app-metadata.properties` (`androidGradlePluginVersion=9.0.1`).

## 5. 16 KB Native Library Alignment
- **Play Console finding**: *Recompile your app with 16 KB native library alignment.*
- **Root cause in 1.0.15**: `io.github.sceneview:sceneview:2.2.1` bundled Filament `1.52.0` `.so` libraries with 4 KB (`0x1000`) ELF `PT_LOAD` segment alignment.
- **Fix in 1.0.16**:
  - Upgraded `io.github.sceneview:sceneview` to `2.3.0` (Filament `1.56.0`).
  - Verified with `python3 scripts/check_android_alignment.py`: all 16 native `.so` libraries in both `app-release.apk` and `app-release.aab` have 16 KB (`0x4000`) ELF `PT_LOAD` alignment and 16 KB ZIP alignment (`0 alignment failures`).

## 6. "देखें" (Preview) and "लगाएँ" (Apply) Buttons Staying On-Screen
- **Issue**: In `1.0.15`, the bottom action row (`देखें` and `लगाएँ` / `अनलॉक`) in `ShrineCustomizer` could be pushed off-screen on devices with system bars/cutouts or larger text scales.
- **Fix in 1.0.16**:
  - Upgraded Compose BOM from `2024.04.01` (`Compose 1.6.5`) to `2025.08.01` (`Compose 1.9.0`) and `activity-compose` to `1.11.0`, fixing `DialogWrapper` full-screen inset and cutout propagation when `decorFitsSystemWindows = false`.
  - Bounded the feedback message area in `ShrineCustomizer` (`Modifier.heightIn(max = 64.dp).verticalScroll(...)`) outside the `Modifier.weight(1f)` options scroll container so error/status text never displaces the action buttons.
  - Added single-line ellipsis safeguarding (`maxLines = 1, overflow = TextOverflow.Ellipsis`) to `RitualChoice` and replaced double bottom navigation bar padding in `BhajanPlayerSheet` with `WindowInsets.safeDrawing`.

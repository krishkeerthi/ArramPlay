# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

Single-module Gradle project (`:app`). Use the wrapper:

- `./gradlew assembleMobileDebug` / `assembleTvDebug` — build debug APK per flavor
- `./gradlew assembleMobileRelease` / `bundleTvRelease` — release (R8 minify + resource shrink enabled)
- `./gradlew installMobileDebug` / `installTvDebug` — install on connected device/emulator
- `./gradlew test` — run JVM unit tests (`app/src/test`)
- `./gradlew testMobileDebugUnitTest --tests "com.krish.videoplayer.ExampleUnitTest.someTest"` — single unit test
- `./gradlew connectedDebugAndroidTest` — instrumented tests (`app/src/androidTest`)
- `./gradlew lint` — Android lint

Toolchain: Gradle 9.8, AGP 9.3 (the newest the installed Android Studio accepts — don't bump past it; built-in Kotlin, so do not apply `org.jetbrains.kotlin.android` to `:app`), Kotlin 2.4 with the Compose compiler plugin. SDK: `compileSdk = 37` (required by current Compose/Navigation), `minSdk = 24`, `targetSdk = 36`. Java/Kotlin target: 17. Version catalog lives in `gradle/libs.versions.toml`. Debug builds use `applicationIdSuffix = ".debug"` so they install next to the Play build.

Flavors (`formFactor`): `mobile` and `tv` — task names are flavored, e.g. `assembleMobileDebug`, `installTvDebug`, `bundleTvRelease`.

## Architecture

Single-Activity Jetpack Compose app for streaming video channels, designed to run on **phones, tablets, and Android TV (Leanback)** from one codebase.

- **Entry point**: `MainActivity` (`com.krish.videoplayer`) hosts a `NavHost` with four type-safe destinations declared as `@Serializable` route classes/objects: `ChannelList` (start), `Channel(channelId: Int)`, `Settings`, `AboutUs`. Navigation uses `kotlinx.serialization` + `navigation-compose`'s `toRoute()`, with shared-axis transitions set on the `NavHost`. Adding a destination means: create a `@Serializable` route type, add a `composable<Route>` block, and pass `navController` down (plus `window` if the screen touches system bars).

- **Form-factor switching**: `ui/DeviceType.kt` resolves `Phone`/`Tablet`/`Tv` once (UiModeManager / leanback feature / `smallestScreenWidthDp`) and exposes it as `LocalDeviceType`. The player route locks phones to `SENSOR_LANDSCAPE` via a `DisposableEffect`; everything else is unspecified. Each flavor has its own `ChannelsPreview.kt` (`src/mobile`, `src/tv`): mobile branches on orientation into `ChannelsPreviewPortrait` (top bar + adaptive grid) vs `ChannelsPreviewLandscape` (immersive backdrop + focusable card row); TV always uses the landscape one with `TvSafePadding` overscan margins. D-pad focus is plain Compose foundation (no `androidx.tv` libs) — `FocusRequester` for initial focus (requested only in keyboard input mode on mobile) and `Modifier.focusHighlight` / `ChannelCard` in `ui/components` for focus visuals. Keep focus wiring intact when editing list UI.

- **Player**: `ChannelUI.ChannelStream` builds an ExoPlayer (Media3) via `buildExoPlayer` (live-tuned `LoadControl`, audio focus, becoming-noisy) and `buildMediaSource`, where the URL scheme picks the source: `rtmp` → `ProgressiveMediaSource` + `RtmpDataSource`, `rtsp` → `RtspMediaSource`, else `DefaultMediaSourceFactory` (HLS). It owns a `MediaSession`, a Compose overlay (controls, channel strip, zap banner, poster-until-first-frame, error panel), auto-retry with backoff + network awareness (`Network.kt`), channel zapping (D-pad up/down, channel/number keys, swipe), and PiP (`MainActivity` auto-enter on S+, source-rect hint). Channels and stream URLs live in `Channels.kt` (`channels`, `channelById`, `getStreamUrl`).

- **Settings**: `settings/AppSettings.kt` — DataStore-backed `SettingsRepository` (theme mode, dynamic color, video scale, resume last channel, auto-PiP, last channel). `MainActivity` collects it, holds the splash until the first read, and provides `LocalAppSettings` / `LocalSettingsRepository`. UI in `SettingsUI.kt`.

- **App update**: `MainActivity.checkForAppUpdate` triggers a Play Core **immediate** in-app update on launch via `AppUpdateOptions` + an ActivityResult launcher; `onResume` resumes an interrupted update. (`androidx.fragment` is pinned because Play Core pulls an old one that fails `lintVital`.)

- **Manifest**: declares `LEANBACK_LAUNCHER` category and `android:banner` so the same APK appears on Android TV launchers; `touchscreen` and `leanback` features are both `required="false"`.

- **Theme**: `ui/theme/` (`Color.kt`, `Theme.kt`, `Type.kt`) — brand red/charcoal light and dark schemes, optional dynamic color. System-bar icon style follows the in-app theme (`MainActivity.applySystemBarStyle`, re-applied after the splash exits). XML window backgrounds in `values/`, `values-night/` and `src/tv/res` must match the Compose background colors to avoid a flash on launch. Splash: `Theme.VideoPlayer.Starting` (core-splashscreen).

## Conventions

- All UI is Compose; no XML layouts beyond manifest/resources.
- Routes are declared as top-level `@Serializable` types in their own files (`Channel.kt`, `ChannelList.kt`, `Settings.kt`, `AboutUs.kt`) — keep this split so `toRoute()` reflection works cleanly.
- When touching list/grid UI, test both portrait (phone) and landscape/TV (D-pad focus) paths — they are separate composables built from the shared pieces in `ui/components`.
- New user-facing strings go in `values/strings.xml` **and** all 16 `values-*/strings.xml` locales.

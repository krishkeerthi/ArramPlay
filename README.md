# VideoPlayer

A single-codebase Jetpack Compose app that streams live RTMP/RTSP video channels on **phones, tablets, and Android TV**. One APK, three form factors — orientation and UI variant are switched per route at runtime.

## Tech stack

- **UI:** Jetpack Compose (Material 3) + `androidx.tv.foundation` / `androidx.tv.material` for the Leanback path
- **Player:** ExoPlayer (Media3 1.11) with RTMP, RTSP and HLS sources, MediaSession, PiP
- **Navigation:** `navigation-compose` with `kotlinx.serialization` type-safe routes
- **Build:** Gradle 9.8 · AGP 9.3 · Kotlin 2.4
- **Min SDK:** 24 · **Target SDK:** 36 · **Compile SDK:** 37 · **Java/Kotlin:** 17

## Setup

### 1. Clone and configure secrets

Stream URLs are not checked into git. Copy the template and fill in real values:

```bash
cp local.properties.example local.properties
# then edit local.properties and set:
#   sdk.dir=...
#   RTMP_HOST=host:port
#   RTMP_DOMAIN=example.com
#   RTMP_PATH_ARRAM=path/livetv
#   RTMP_PATH_RR=path/livetv
#   RTMP_PATH_LIVEUPDATES=path/livetv
```

`local.properties` is gitignored. The values are read at build time by `app/build.gradle.kts` and exposed to Kotlin via the generated `BuildConfig` class — `Channels.getStreamUrl` reads them from there.

### 2. Open in Android Studio

Open the project root and let Gradle sync. After sync, `BuildConfig.RTMP_*` fields will resolve.

## Build & run

The project uses the Gradle wrapper. Common commands:

| Command | What it does |
| --- | --- |
| `./gradlew assembleMobileDebug` | Build debug APK for phone/tablet flavor |
| `./gradlew assembleTvDebug` | Build debug APK for the Android TV flavor |
| `./gradlew assembleMobileRelease` | Build minified release APK (R8 + resource shrink) |
| `./gradlew installMobileDebug` | Install debug APK on the connected device |
| `./gradlew test` | Run JVM unit tests (`app/src/test`) |
| `./gradlew connectedDebugAndroidTest` | Run instrumented tests (`app/src/androidTest`) |
| `./gradlew lint` | Android lint |

There are two product flavors under `formFactor`:

- `mobile` — keeps the original `applicationId`; this is what's on the Play listing.
- `tv` — adds `.tv` suffix and a `-tv` versionName, so phone and TV builds can coexist on the same device.

## Architecture overview

Single Activity, single NavHost. Three destinations, each declared as a top-level `@Serializable` route type so `navigation-compose`'s `toRoute()` reflection works:

- `ChannelList` — start destination, grid of channels.
- `Channel(channelId: Int)` — full-screen player.
- `Settings` — theme, playback and about options.
- `AboutUs`.

The player route locks phones to landscape; everything else uses unspecified. `ui/DeviceType.kt` resolves phone / tablet / TV. `ChannelListUI` provides `ChannelsPreviewPortrait` (phone portrait grid) and `ChannelsPreviewLandscape` (immersive lean-back home used by landscape phones, tablets and TV) — D-pad focus uses Compose foundation focus APIs and `FocusRequester`.

Adding a new screen: create a `@Serializable` route type in its own file, add a `composable<Route>` block in `MainActivity`, pass `navController` and `window` through.

## 16 KB page-size compatibility (Android 15+)

This project ships **locally built, 16 KB-aligned** native libraries for the RTMP datasource. `io.antmedia:rtmp-client:3.2.0` (a transitive dependency of `androidx.media3:media3-datasource-rtmp:1.10.0`) publishes a `librtmp-jni.so` aligned to 4 KB, which fails to load on Android 15+ devices running with 16 KB pages.

The project repo therefore contains:

- `app/src/main/jniLibs/{arm64-v8a,armeabi-v7a,x86,x86_64}/librtmp-jni.so` — locally compiled, 16 KB-aligned.
- `packaging.jniLibs.pickFirsts` rules in `app/build.gradle.kts` — make our copy win duplicate resolution against the AAR's bundled `.so`.

To verify any APK is 16 KB-clean:

```bash
unzip -oq path/to/app.apk -d /tmp/apk-check
for so in $(find /tmp/apk-check/lib -name "*.so"); do
  echo "=== $so ==="
  llvm-readelf -l "$so" | grep LOAD
done
# Every LOAD line's last column (Align) must be 0x4000 or larger.
```

### Rebuilding `librtmp-jni.so` from source

When upstream eventually publishes `io.antmedia:rtmp-client:3.2.1+` with 16 KB alignment, you can delete `app/src/main/jniLibs/` and the `pickFirsts` block. Until then, to rebuild the libs locally (e.g. on a new machine):

```bash
# Prerequisites: NDK r29 + CMake
brew install --cask android-ndk
brew install cmake
export ANDROID_NDK_HOME=/opt/homebrew/share/android-ndk

# Fetch and patch the source
git clone --depth 1 https://github.com/ant-media/LibRtmp-Client-for-Android.git /tmp/librtmp
# Patch 1: bump cmake_minimum_required to 3.10 (CMake 4.x dropped <3.5)
sed -i '' 's/cmake_minimum_required(VERSION 3.4.1)/cmake_minimum_required(VERSION 3.10)/' \
  /tmp/librtmp/rtmp-client/CMakeLists.txt
# Patch 2: NDK r29's stricter clang needs an explicit memcpy include
sed -i '' '/^#include <stdlib.h>/a\
#include <string.h>
' /tmp/librtmp/rtmp-client/src/main/cpp/flvmuxer/xiecc_rtmp.c

# Build all four ABIs
SRC=/tmp/librtmp/rtmp-client
OUT=/tmp/rtmp-build
for ABI in arm64-v8a armeabi-v7a x86 x86_64; do
  cmake -S "$SRC" -B "$OUT/$ABI" \
    -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI="$ABI" \
    -DANDROID_PLATFORM=android-24 \
    -DCMAKE_BUILD_TYPE=Release
  cmake --build "$OUT/$ABI" -j 4
done

# Copy outputs into the project
for ABI in arm64-v8a armeabi-v7a x86 x86_64; do
  cp "$OUT/$ABI/librtmp-jni.so" "app/src/main/jniLibs/$ABI/librtmp-jni.so"
done
```

The 16 KB alignment comes from `-Wl,-z,max-page-size=16384` in upstream's `rtmp-client/CMakeLists.txt` — already in the source, just never published as a binary release.

## Project layout

```
app/
├── build.gradle.kts             # flavors, BuildConfig wiring, jniLibs packaging
├── src/main/
│   ├── AndroidManifest.xml      # Leanback launcher + touch/leanback features optional
│   ├── java/com/krish/videoplayer/
│   │   ├── MainActivity.kt      # NavHost, orientation handling, in-app update
│   │   ├── ChannelList.kt       # @Serializable route
│   │   ├── Channel.kt           # @Serializable route
│   │   ├── AboutUs.kt           # @Serializable route
│   │   ├── Channels.kt          # channel list + stream URLs
│   │   ├── ChannelListUI.kt     # phone (portrait) + tablet/TV (landscape) home
│   │   ├── ChannelUI.kt         # player: ExoPlayer, overlay, zapping, retry, PiP
│   │   ├── SettingsUI.kt        # settings screen
│   │   ├── settings/            # DataStore-backed AppSettings
│   │   ├── ui/components/       # shared cards, LIVE badge, focus visuals
│   │   └── ui/theme/            # Material 3 theme
│   └── jniLibs/                 # 16 KB-aligned librtmp-jni.so per ABI
gradle/libs.versions.toml         # version catalog
local.properties.example          # template — copy to local.properties and fill in
```

## Conventions

- All UI is Compose; no XML layouts beyond manifest/resources.
- Route types live in their own files (`Channel.kt`, `ChannelList.kt`, `AboutUs.kt`).
- List/grid UIs are **separate composables per form factor**, not a single responsive layout. When editing one, test the other — phone portrait and TV D-pad focus are independent code paths.

## Releasing

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`.
2. `./gradlew bundleMobileRelease` → `app/build/outputs/bundle/mobileRelease/*.aab` for the phone listing.
3. `./gradlew bundleTvRelease` for the TV listing.
4. Upload to Google Play Console. The console will report 16 KB native-code support — verify it shows green for both bundles.

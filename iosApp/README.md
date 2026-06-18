# Finance SLM — iOS app

SwiftUI front-end for the Kotlin Multiplatform `shared` module. The app is
generated with [XcodeGen](https://github.com/yonkov/xcodegen) so the project is
checked in as a readable `project.yml` rather than a fragile `.xcodeproj`.

> **Requires macOS + Xcode.** None of this can be built on Linux (Kotlin/Native
> Apple targets, the llama.cpp build, and Xcode are all macOS-only).

## Build & run

```bash
# 1. Build llama.cpp static libs for iOS (device + simulator), once.
bash ../scripts/build-llama-ios.sh

# 2. Generate the Xcode project.
brew install xcodegen        # if not already installed
cd iosApp
xcodegen generate

# 3. Open and run.
open FinanceSLM.xcodeproj
#   Select an iOS Simulator (Apple Silicon) and Run.
```

The project's pre-build script runs `./gradlew :shared:embedAndSignAppleFrameworkForXcode`,
which compiles the `Shared` framework (with the llama cinterop) for the active
configuration/SDK and places it where the app's `FRAMEWORK_SEARCH_PATHS` expects.

## What works

- Koin DI bootstrapped from Swift via `KoinIosKt.doInitKoinIos()`.
- Model catalog loaded from the bundled `model_catalog.json`.
- Model download with live progress (Ktor/Darwin + okio + SHA-256).
- On-device inference through the llama.cpp cinterop (`IosLlamaEngine`).
- Local persistence (SQLDelight native driver) and preferences (NSUserDefaults).

## Known limitations / things to verify in Xcode

- **Screen reading is Android-only.** iOS sandboxing has no equivalent of
  Android's AccessibilityService, so the iOS app uses manual text input on the
  Insights tab instead of reading other apps.
- **Kotlin↔Swift interop names** can vary by toolchain. A few spots are likely
  to need a small tweak when you first build: sealed `DownloadState` subclass
  names (`DownloadStateDownloading` etc.), the `[ModelInfo]` array bridging, and
  `ModelInfo.description_` (Kotlin `description` is renamed to avoid the
  `NSObject.description` clash).
- **llama.cpp C symbols** (especially the LoRA adapter API in
  `LlamaEngine.ios.kt`) track a specific submodule version and may need minor
  adjustment against the headers you build against.
- First bring-up uses a **CPU-only** llama build (`GGML_METAL=OFF`). Flip it on
  in `scripts/build-llama-ios.sh` later for GPU acceleration.

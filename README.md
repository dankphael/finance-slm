# Finance SLM

An Android app that provides personalized financial insights using a locally-hosted Small Language Model (SLM) via [llama.cpp](https://github.com/ggml-org/llama.cpp). All inference runs on-device — no data ever leaves your phone.

## What It Does

1. **Screen Reading** — Monitors supported Singapore finance apps (DBS/PayNow, PayLah, OCBC, UOB, Grab, Moomoo, Tiger Brokers) via Android AccessibilityService to extract financial data
2. **AI Insights** — Runs a local SLM (Qwen 2.5 0.5B/1.5B/3B) to generate personalized financial tips based on your spending patterns
3. **LoRA Personas** — Customize the AI's persona with prompt-based LoRA adapters (no weight files needed)
4. **Model Management** — Download, switch, and manage multiple GGUF models with resume support and SHA256 verification
5. **Privacy-First** — All data stays on-device. Export or delete all data at any time.

## Architecture

```
┌─────────────────────────────────────────────────┐
│  Android App (Jetpack Compose + Material 3)     │
│  ┌───────────┬──────────┬───────────────────┐   │
│  │ Insights  │  LoRA    │     Settings      │   │
│  │   Tab     │  Tab     │      Tab          │   │
│  └─────┬─────┴────┬─────┴────────┬──────────┘   │
│        │          │              │               │
│  ┌─────▼──────────▼──────────────▼──────────┐   │
│  │           ViewModels (Koin DI)           │   │
│  └─────────────────┬───────────────────────┘   │
│                    │                            │
│  ┌─────────────────▼───────────────────────┐   │
│  │     KMP Shared Module (commonMain)       │   │
│  │  ┌──────────┬──────────┬──────────────┐  │   │
│  │  │ Domain   │  Data    │  Inference   │  │   │
│  │  │ Models   │  Repos   │  LlamaEngine │  │   │
│  │  └──────────┴──────────┴──────┬───────┘  │   │
│  └───────────────────────────────┼──────────┘   │
│                                  │               │
│  ┌───────────────────────────────▼──────────┐   │
│  │  JNI Bridge (llama_jni.cpp)              │   │
│  │  ┌─────────────┬──────────────────────┐  │   │
│  │  │ libllama.so │  Crash Handler       │  │   │
│  │  └─────────────┴──────────────────────┘  │   │
│  └──────────────────────────────────────────┘   │
└─────────────────────────────────────────────────┘
```

### Tech Stack

| Layer | Technology |
|-------|-----------|
| **UI** | Jetpack Compose, Material 3, Navigation Compose |
| **Architecture** | MVVM, Koin DI, Kotlin Coroutines + Flow |
| **Shared Logic** | Kotlin Multiplatform (KMP) |
| **Database** | SQLDelight (insights persistence) |
| **Inference** | llama.cpp via JNI (libllama.so) |
| **Model Download** | Ktor HTTP, WorkManager (foreground service) |
| **Build** | Gradle 8.7, Kotlin 2.1, AGP 8.7.3 |
| **CI/CD** | GitHub Actions |

## Project Structure

```
finance-slm/
├── androidApp/                          # Android application module
│   ├── src/main/
│   │   ├── java/com/habibi/financeslm/android/
│   │   │   ├── di/                      # Koin DI modules
│   │   │   ├── service/
│   │   │   │   ├── FinanceScreenReaderService.kt   # AccessibilityService
│   │   │   │   └── ModelDownloadWorker.kt          # WorkManager download
│   │   │   ├── ui/
│   │   │   │   ├── home/                # HomeScreen (Insights/LoRA/Settings tabs)
│   │   │   │   ├── onboarding/          # Model selection & download
│   │   │   │   └── settings/            # Model management, LoRA editor
│   │   │   ├── viewmodel/               # HomeViewModel, OnboardingViewModel, etc.
│   │   │   ├── data/repository/         # Android-specific DownloadEnqueuer
│   │   │   └── FinanceSlmApp.kt         # Application class
│   │   ├── assets/
│   │   │   └── model_catalog.json       # Available GGUF models
│   │   └── res/                         # Android resources
│   └── build.gradle.kts
│
├── shared/                              # KMP shared module
│   ├── src/commonMain/
│   │   ├── kotlin/com/habibi/financeslm/
│   │   │   ├── data/
│   │   │   │   ├── datasource/          # ModelDownloadDataSource, PreferencesDataSource
│   │   │   │   ├── repository/          # Repository implementations
│   │   │   │   └── mapper/              # JSON mappers
│   │   │   ├── domain/
│   │   │   │   ├── model/               # FinanceInsight, ModelInfo, LoraAdapter, etc.
│   │   │   │   ├── repository/          # Repository interfaces
│   │   │   │   └── usecase/             # GenerateInsightUseCase, ManageLoraUseCase
│   │   │   ├── inference/               # LlamaEngine interface, InferenceParams
│   │   │   ├── prompt/                  # PromptBuilder, PromptTemplate
│   │   │   ├── platform/                # expect/actual PlatformContext, FileSystem
│   │   │   └── util/                    # Logger, ChecksumVerifier, SingleThreadDispatcher
│   │   └── sqldelight/                  # SQLDelight schemas
│   ├── src/androidMain/
│   │   ├── cpp/
│   │   │   ├── llama_jni.cpp            # JNI bridge to llama.cpp
│   │   │   ├── crash_handler.cpp        # Native crash signal handlers
│   │   │   └── CMakeLists.txt
│   │   └── kotlin/                      # Android actual implementations
│   ├── src/iosMain/                     # iOS actuals (cinterop llama.cpp, Ktor/okio)
│   ├── src/nativeInterop/cinterop/      # llama.def (iOS cinterop binding)
│   └── build.gradle.kts
│
├── iosApp/                              # SwiftUI app (XcodeGen project.yml) — build on macOS
├── scripts/build-llama-ios.sh          # Builds llama.cpp static libs for iOS
├── docs/
│   └── review-and-enhancements.md       # Architecture review & enhancement proposals
│
├── .github/workflows/build.yml          # CI/CD
├── gradle/libs.versions.toml            # Version catalog
└── settings.gradle.kts
```

## Getting Started

### Prerequisites

- Android SDK 34, NDK `26.1.10909125`, CMake `3.22.1`
- Java 17 (Temurin recommended)
- Kotlin 2.1+

`llama.cpp` is compiled from source automatically by the Gradle build (via the
shared module's CMake `externalNativeBuild`), so there is **no manual native
build step** — you only need the submodule checked out and the NDK installed.

### Build

```bash
# Clone with the llama.cpp submodule
git clone --recurse-submodules https://github.com/dankphael/finance-slm.git
cd finance-slm
# (if you already cloned without --recurse-submodules)
git submodule update --init --depth 1 llama-cpp

# Build debug APK — this also cross-compiles libllama.so + libllamajni.so
./gradlew :androidApp:assembleDebug

# Output: androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

The first build is slow (it compiles llama.cpp + ggml for arm64-v8a);
subsequent builds reuse the cached native output.

### Release builds & signing

Release signing is read from a git-ignored `keystore.properties` at the project
root. Copy the template and fill in your keystore details:

```bash
cp keystore.properties.example keystore.properties
# generate a keystore if you don't have one:
keytool -genkeypair -v -keystore release.jks -alias finance-slm \
    -keyalg RSA -keysize 2048 -validity 10000

./gradlew :androidApp:assembleRelease
```

If `keystore.properties` is absent, release builds fall back to debug signing
so `assembleRelease` still succeeds (e.g. in CI). Release builds run R8/minify
with the keep rules in `androidApp/proguard-rules.pro`.

### Running Tests

```bash
# Compile and run common tests
./gradlew :shared:test
```

### Model Setup

Models are defined in `androidApp/src/main/assets/model_catalog.json`. The app downloads GGUF models from HuggingFace on first use. Three Qwen 2.5 variants are pre-configured:

| Model | Size | RAM | Context |
|-------|------|-----|---------|
| Qwen 2.5 0.5B Q4_K_M | ~376 MB | 1 GB | 2048 |
| Qwen 2.5 1.5B Q4_K_M | ~940 MB | 2 GB | 4064 |
| Qwen 2.5 3B Q4_K_M | ~1.8 GB | 4 GB | 8192 |

## Key Features

### Screen Reading
- Monitors 7 Singapore finance apps via AccessibilityService
- Extracts amounts, dates, balances, and transaction descriptions
- Skips password fields and notification events (OTP protection)
- Iterative tree traversal (no stack overflow on deeply nested UIs)

### On-Device Inference
- Runs GGUF models via llama.cpp JNI bridge
- Single-thread dispatcher (llama.cpp is not thread-safe)
- Real-time token streaming to UI
- Native crash signal handlers (SIGSEGV/SIGABRT → graceful error)
- Model-specific chat templates (Qwen, Llama, Mistral)

### LoRA Personas
- Prompt-based LoRA adapters (no weight files needed)
- Create, edit, delete, and switch between personas
- Pre-seeded "Default Finance Advisor" on first launch

### Data & Privacy
- All data stored locally via SQLDelight
- Export all insights as JSON
- Delete all data with one tap (Play Store compliance)
- In-app link to the [privacy policy](PRIVACY.md) from Settings
- AccessibilityService self-excludes own package

## Development

### Architecture Decisions

- **KMP expect/actual** for platform boundaries (LlamaEngine, PlatformContext, FileSystem, ChecksumVerifier)
- **JNI_OnLoad + RegisterNatives** (not name-mangling) for stable native bindings
- **WorkManager** for model downloads (survives app backgrounding)
- **SQLDelight** for type-safe SQL with coroutines Flow support
- **Koin** for dependency injection

### Model integrity (SHA256)

`model_catalog.json` ships with empty `sha256` fields, so download verification
is skipped. To enforce integrity, populate the hashes:

```bash
bash scripts/compute-model-checksums.sh   # downloads each model and prints its SHA256
```

Paste each value into the matching model's `sha256` field; the app then
verifies every download and rejects corrupted/tampered files.

### iOS

iOS is now a buildable target (build on macOS — see [`iosApp/README.md`](iosApp/README.md)):
a consumable `Shared` framework, real Kotlin/Native platform implementations
(Foundation paths, okio filesystem + SHA-256, Ktor/Darwin downloads), llama.cpp
inference via cinterop, and a SwiftUI app. Screen-reading insights remain
Android-only (iOS sandboxing has no AccessibilityService equivalent); the iOS
app uses manual text input instead. The shared module and the SwiftUI app cannot
be compiled on Linux/CI — they require a Mac + Xcode.

### Known Issues

- iOS on-device inference requires building the llama.cpp static libs on a Mac
  first (`scripts/build-llama-ios.sh`); a few Kotlin↔Swift interop names and
  llama.cpp C symbols may need minor Xcode-side adjustment (documented in
  `iosApp/README.md`).

## License

Private project. All rights reserved.

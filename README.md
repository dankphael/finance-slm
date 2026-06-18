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
│   ├── src/iosMain/                     # iOS stubs (future)
│   └── build.gradle.kts
│
├── docs/
│   └── review-and-enhancements.md       # Architecture review & enhancement proposals
│
├── .github/workflows/build.yml          # CI/CD
├── gradle/libs.versions.toml            # Version catalog
└── settings.gradle.kts
```

## Getting Started

### Prerequisites

- Android SDK 34, NDK 26.1
- Java 17 (Temurin recommended)
- Kotlin 2.1+
- llama.cpp prebuilt `libllama.so` for arm64-v8a (see [Building llama.cpp](#building-llamacpp))

### Build

```bash
# Clone with submodules
git clone --recurse-submodules https://github.com/dankphael/finance-slm.git
cd finance-slm

# Build debug APK
./gradlew :androidApp:assembleDebug

# Output: androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

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
- AccessibilityService self-excludes own package

## Development

### Architecture Decisions

- **KMP expect/actual** for platform boundaries (LlamaEngine, PlatformContext, FileSystem, ChecksumVerifier)
- **JNI_OnLoad + RegisterNatives** (not name-mangling) for stable native bindings
- **WorkManager** for model downloads (survives app backgrounding)
- **SQLDelight** for type-safe SQL with coroutines Flow support
- **Koin** for dependency injection

### Known Issues

- `LoraRepositoryImpl.kt` uses `System.currentTimeMillis()` in commonMain (JVM-only API) — needs `expect/actual` for clock
- iOS module is stubbed (Android-first development)

## License

Private project. All rights reserved.

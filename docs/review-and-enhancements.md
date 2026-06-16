# Finance SLM App — Comprehensive Architecture & Code Review

**Reviewer:** Pat (AI Technical Team Lead)  
**Date:** 16 June 2026  
**Scope:** Full codebase — 81 Kotlin files (~4,876 LOC) + 2 C++ files (llama_jni.cpp: 378 LOC) across shared module + androidApp  
**Phases Complete:** 0–7 (all stubs replaced with real implementations)

---

## Table of Contents

1. [Part A: Architecture Review](#part-a-architecture-review)
   - [A1. What's Well-Designed](#a1-whats-well-designed)
   - [A2. Technical Debt & Risks](#a2-technical-debt--risks)
   - [A3. Production Readiness Gaps](#a3-production-readiness-gaps)
   - [A4. Security & Privacy Review](#a4-security--privacy-review)
2. [Part B: Code Quality Review](#part-b-code-quality-review)
   - [B1. JNI Bridge Correctness & Safety](#b1-jni-bridge-correctness--safety)
   - [B2. Threading Model](#b2-threading-model)
   - [B3. Error Handling](#b3-error-handling)
   - [B4. Memory Management](#b4-memory-management)
   - [B5. AccessibilityService Lifecycle](#b5-accessibilityservice-lifecycle)
3. [Part C: Enhancement Proposals](#part-c-enhancement-proposals)

---

# Part A: Architecture Review

## A1. What's Well-Designed

The following architectural decisions are solid and should be preserved:

### 1. KMP expect/actual separation (4/5)
The architecture doc's `expect/actual` strategy is correctly applied to the right boundaries:
- `LlamaEngine` (expect in commonMain, JNI in androidMain, cinterop stub in iosMain) — correct
- `PlatformContext`, `FileSystem`, `ScreenReader` — each properly separated
- `ChecksumVerifier` — expect/actual for platform-specific hash computation

**Files:** `shared/src/commonMain/kotlin/com/habibi/financeslm/inference/LlamaEngine.kt` (line 10, interface), `shared/src/androidMain/.../LlamaEngine.android.kt` (line 15, `class LlamaEngineAndroid : LlamaEngine`)

### 2. JNI_OnLoad + RegisterNatives pattern (5/5)
Following the architecture doc's Gotcha G1, `llama_jni.cpp` uses `JNI_OnLoad` + `RegisterNatives` (lines 355-378) instead of name-mangled JNI function names. This is the correct approach — renaming the Kotlin class won't break native bindings.

**File:** `shared/src/androidMain/cpp/llama_jni.cpp`, lines 322-378

### 3. AccessibilityService package filtering (5/5)
The service correctly filters by package name at two levels:
- **System level:** `serviceInfo.packageNames` restricts events to the 7 allowed finance apps (line 112)
- **Code level:** Double-check in `onAccessibilityEvent` (line 125)
- **Privacy:** Skips password fields via `node.isPassword` (line 183)
- **Self-exclusion:** Explicitly excludes own package (line 64: `OWN_PACKAGE`)

**File:** `androidApp/src/main/java/com/habibi/financeslm/android/service/FinanceScreenReaderService.kt`, lines 98-113, 119-161

### 4. ScreenReaderBridge singleton pattern (4/5)
Using a `SharedFlow`-based singleton bridge between the AccessibilityService and main app is architecturally correct. Both run in the same process, so in-memory is fine.

**File:** `shared/src/androidMain/kotlin/com/habibi/financeslm/service/ScreenReaderBridge.kt`

### 5. Model download with resume + SHA256 (4/5)
The download pipeline supports HTTP Range headers for resume (line 81-83) and SHA256 verification (line 152-163). The design is sound — though the SHA256 fields are empty in practice (see risks below).

**File:** `shared/src/commonMain/kotlin/com/habibi/financeslm/data/datasource/ModelDownloadDataSource.kt`

### 6. Clean domain layer (5/5)
Domain models (`FinanceInsight`, `ScreenData`, `ModelInfo`, `LoraAdapter`, `DownloadState`) are clean data classes with no platform dependencies. Repository interfaces are properly abstracted. Use cases (`GenerateInsightUseCase`) correctly orchestrate between repositories.

**Files:** `shared/src/commonMain/kotlin/com/habibi/financeslm/domain/model/*.kt`, `shared/src/commonMain/kotlin/com/habibi/financeslm/domain/repository/*.kt`

### 7. DI wiring via Koin (4/5)
Koin modules (`appModule`, `sdkModule`, `androidAppModule`) correctly wire dependencies. The `FinanceSlmApp` Application class initializes PlatformContext before Koin.

**Files:** `androidApp/src/main/java/com/habibi/financeslm/android/di/AndroidAppModule.kt`, `androidApp/src/main/java/com/habibi/financeslm/android/FinanceSlmApp.kt`

---

## A2. Technical Debt & Risks

### R1: CRITICAL — Single-thread dispatcher NOT enforced
**Severity:** Critical (potential native crash / undefined behavior)

The architecture doc's Gotcha G4 states: *"The inference engine MUST use a dedicated single-thread dispatcher. llama.cpp is NOT thread-safe."*

**Actual implementation:** `InferenceRepositoryImpl` line 36:
```kotlin
private val inferenceDispatcher = Dispatchers.Default
```

`Dispatchers.Default` is a thread pool backed by `max(2, CPU cores)` threads. On a modern phone with 8 cores, that's 8 threads. Multiple concurrent coroutines could call `llama_generate` simultaneously on the same `g_context`, causing data corruption or SIGSEGV.

**Also:** `LlamaEngineAndroid` uses `withContext(Dispatchers.Default)` directly in `loadModel`, `infer`, etc. (lines 61, 76, 87, 105, 110) — same problem.

**Fix:** Should be `newSingleThreadContext("llama-inference")` and properly closed on teardown.

### R2: CRITICAL — ModelDownloadDataSource in commonMain uses JVM-only APIs
**Severity:** Critical (iOS compilation failure)

`ModelDownloadDataSource.kt` is located at:
```
shared/src/commonMain/kotlin/com/habibi/financeslm/data/datasource/ModelDownloadDataSource.kt
```

But it imports:
- `java.net.HttpURLConnection` (line 18)
- `java.net.URL` (line 19)
- `java.io.BufferedInputStream` (line 14)
- `java.io.File` (line 16)

These are JVM-only APIs. In a KMP project, this file would fail to compile for `iosArm64` targets. The architecture doc explicitly states the download should use Ktor (`ktor-client-core`), which is KMP-compatible.

**Fix:** Either move this file to `androidMain` (with an `expect`/`actual` split in commonMain) or rewrite using Ktor `HttpClient`.

### R3: HIGH — Model catalog has empty SHA256 checksums
**Severity:** High (no download integrity verification)

```json
// model_catalog.json lines 10, 19, 31
"sha256": ""
```

All three models have empty SHA256. The `ModelDownloadDataSource` checks `if (expectedSha256.isNotEmpty())` before verifying (line 48), so verification is silently skipped for all downloads. A corrupted or tampered model file would go undetected.

### R4: HIGH — PromptTemplate hardcodes Qwen-specific chat format
**Severity:** High (wrong output for non-Qwen models)

```kotlin
// PromptTemplate.kt lines 14-18
fun renderChatTemplate(...): String {
    return """<|im_start|>system
$systemPrompt<|im_end|>
<|im_start|>user
$userPrompt<|im_end|>
<|im_start|>assistant
"""
}
```

The `<|im_start|>` tokens are Qwen-specific. If a Llama, Mistral, or Phi model is downloaded, the chat template won't produce correct results. The model catalog includes `"qwen2.5-3b"`, so Qwen alignment is assumed, but the architecture needs template-per-model support.

### R5: MEDIUM — Inference emits entire output as single token
**Severity:** Medium (UI appears frozen during generation)

Despite `LlamaEngine.infer()` returning `Flow<String>`, the implementation emits only ONCE:

```kotlin
// LlamaEngine.android.kt lines 84-103
override suspend fun infer(prompt: String, params: InferenceParams): Flow<String> = flow {
    val result = withContext(Dispatchers.Default) {
        nativeGenerate(...)  // SYNCHRONOUS — blocks until all tokens generated
    }
    if (result.isNotEmpty()) {
        emit(result)  // Single emission of entire output
    }
}
```

The JNI `nativeGenerate` function (llama_jni.cpp line 113) runs the full generation loop synchronously inside the JNI call. The Kotlin side receives the complete string and emits it once. There is NO real token streaming — the UI shows nothing until the entire generation completes (5-30 seconds).

### R6: MEDIUM — No persistent storage for insights
**Severity:** Medium (data lost on app restart)

```kotlin
// InferenceRepositoryImpl.kt line 38
private val _insights = MutableStateFlow<List<FinanceInsight>>(emptyList())
```

Insights are stored only in an in-memory `MutableStateFlow`. When the app is killed, all generated insights are lost. The architecture doc mentions SQLDelight for "extracted screen data" and insight storage, but SQLDelight isn't wired up in the current implementation.

### R7: MEDIUM — JNI global state is unsynchronized
**Severity:** Medium (race condition on model load/unload)

```cpp
// llama_jni.cpp lines 21-22
static struct llama_model   * g_model   = nullptr;
static struct llama_context * g_context = nullptr;
```

These are global static pointers with no mutex/lock protection. If `loadModel` and `generate` are called simultaneously from different threads, the model could be freed while `generate` is reading from it, causing a use-after-free SIGSEGV.

### R8: MEDIUM — LoraEditorScreen has dead UI
**Severity:** Medium (broken feature)

```kotlin
// ModelManagementScreen.kt lines 316-345
OutlinedTextField(value = "", onValueChange = {}, ...)  // Dead field
OutlinedTextField(value = "", onValueChange = {}, ...)  // Dead field
Button(onClick = onBack, ...) { Text("Save (Back)") }    // No actual save
```

The LoraEditorScreen has hardcoded empty text fields and a "Save" button that simply navigates back without saving. This was marked as stub but should have been completed in Phase 7.

### R9: LOW — AccessibilityService recursion depth risk
**Severity:** Low (potential StackOverflowError on deeply nested UIs)

```kotlin
// FinanceScreenReaderService.kt lines 181-203
private fun collectText(node: AccessibilityNodeInfo, results: MutableList<String>) {
    for (i in 0 until node.childCount) {
        val child = node.getChild(i) ?: continue
        collectText(child, results)  // Recursive
        child.recycle()
    }
}
```

Heavily nested UIs (e.g., nested RecyclerViews inside ViewPagers) could trigger StackOverflowError. Should use iterative traversal with an explicit stack.

### R10: LOW — PlatformContext hardcodes file paths
**Severity:** Low (portability concern)

```kotlin
// PlatformContext.android.kt lines 7-8
actual val filesDir: String = "/data/data/com.habibi.financeslm/files"
actual val cacheDir: String = "/data/data/com.habibi.financeslm/cache"
```

Should use `androidContext.filesDir.absolutePath` instead of hardcoding. On some devices (dual apps, work profiles), the actual path may differ.

---

## A3. Production Readiness Gaps

### Missing for a production-ready app:

| Area | Gap | Severity |
|------|-----|----------|
| **Testing** | No unit tests in `shared/src/commonTest/` for key components. Test directory structure exists in the architecture plan but wasn't populated. | High |
| **Crash reporting** | No crash reporting (Firebase Crashlytics or similar). Native crashes in llama.cpp are silent. | High |
| **Analytics-free telemetry** | No opt-in usage metrics for understanding feature adoption and model performance. | Medium |
| **CI/CD** | No GitHub Actions or CI pipeline for automated builds and testing. | Medium |
| **Play Store compliance** | `accessibility_service_config.xml` has `typeNotificationStateChanged` flag but no `canRetrieveWindowContent` attribute in the XML (it's only set programmatically in `onServiceConnected`). Play Store may flag this. | Medium |
| **Model download resumption** | Resume works in the code, but there's no foreground service/WorkManager — if the app is killed mid-download, the partial file is orphaned. | High |
| **LoRA editing persistence** | LoraEditorScreen UI is dead; LoRA edits don't actually save. | Medium |
| **Model switching** | No logic to unload current model before loading a new one. If user switches models while one is loaded, the old model stays in memory. | Medium |

---

## A4. Security & Privacy Review

### What's done correctly:

1. **`allowBackup="false"`** in AndroidManifest.xml line 12 — prevents backup of sensitive financial data
2. **Password field filtering** — `node.isPassword` check at line 183 of FinanceScreenReaderService
3. **Package-name filtering** — both system-level and code-level filtering restricts data capture to 7 known finance apps
4. **On-device only** — no network calls send user data anywhere; all processing is local
5. **No analytics by default** — aligning with privacy-first architecture

### What needs attention:

| Issue | File | Line | Severity |
|-------|------|------|----------|
| AccessibilityService could read OTP notifications | `accessibility_service_config.xml` | 4 | Medium |
| The `typeNotificationStateChanged` flag means the service receives notification events. Some banking apps send OTPs as notifications. The code doesn't filter notification content — it extracts text from ALL event types within allowed packages. | `FinanceScreenReaderService.kt` | 100 | Medium |
| **Fix:** Add explicit filtering for `event.eventType` — skip `TYPE_NOTIFICATION_STATE_CHANGED` events, or add content filtering for common OTP patterns (6-digit codes). | | | |
| Screen data held in memory indefinitely | `ScreenDataRepositoryImpl.kt` | 40 | Low |
| The cache stores up to 100 entries (`MAX_CACHED_ENTRIES = 100`) with no TTL or clearing mechanism. If the user monitors banking apps for months, sensitive text remains in memory. Should add auto-clear after N days. | | | |
| No data export/delete functionality | — | — | Medium |
| For Play Store compliance (GDPR-like requirements), users need ability to view and delete stored data. Currently no UI for this beyond `clearInsights()` in ViewModel. | | | |

**Play Store scrutiny risk assessment:** The AccessibilityService + finance app combination is a sensitive permission pair. Google will scrutinize the Play Store listing. Mitigation: prominent in-app explanation (already partially in PermissionsManagementScreen lines 401-417), video demo, and a privacy policy explaining exactly what data is captured and why.

---

# Part B: Code Quality Review

## B1. JNI Bridge Correctness & Safety

### What's correct:

1. **RegisterNatives pattern** (llama_jni.cpp lines 322-378): Proper method descriptors, JNI version negotiation, error handling in `JNI_OnLoad`.
2. **Resource cleanup in loadModel** (lines 49-51): Free previous model/context before loading new one.
3. **EOS detection** (lines 193-196): Proper end-of-sequence token checking.
4. **Sampler chain** (lines 175-183): Correct sampler initialization and cleanup (`llama_sampler_free` at line 214).
5. **Tokenize buffer resizing** (lines 152-162): Handles the case where initial buffer is too small (negative return from `llama_tokenize`).

### What needs fixing:

| Issue | File | Lines | Severity |
|-------|------|-------|----------|
| **No error logging for `llama_backend_init` failure** | `llama_jni.cpp` | 54 | Medium |
| `llama_backend_init()` returns a status code, but it's not checked. If backend init fails, the subsequent `llama_model_load_from_file` will fail with a confusing error. | | | |
| **No model size check** | `llama_jni.cpp` | 61 | Medium |
| The model file is loaded without checking file size. If the GGUF is truncated (partial download), `llama_model_load_from_file` may crash or return corrupted model. Should verify file size before loading. | | | |
| **JNI global references leak** | `llama_jni.cpp` | 125 | Low |
| `env->NewStringUTF()` in error paths creates JNI local references that are auto-freed, but in the generate loop (line 217), the result string is returned as a local ref — this is correct. | | | |
| **Static globals with no teardown** | `llama_jni.cpp` | 21-22 | Medium |
| `g_model` and `g_context` are never nullified on library unload. If `libllamajni.so` is unloaded and reloaded, stale pointers remain. Add `JNI_OnUnload` to clean up. | | | |
| **llama_backend_free never called** | `llama_jni.cpp` | — | Medium |
| `llama_backend_init()` is called (line 54) but `llama_backend_free()` is never called. This leaks backend resources. Should be called in a `JNI_OnUnload` handler or a dedicated `nativeFreeBackend` function. | | | |

### JNI method signature verification:

The RegisterNatives table (lines 322-353) maps correctly to the Kotlin extern declarations in `LlamaEngineAndroid`:

| Kotlin extern | JNI method name | Descriptor | Match |
|--------------|-----------------|------------|-------|
| `nativeLoadModel(String, Int, Int, Int, Int): Boolean` | `nativeLoadModel` | `(Ljava/lang/String;IIII)Z` | ✓ |
| `nativeFreeModel()` | `nativeFreeModel` | `()V` | ✓ |
| `nativeGenerate(String, Int, Float, Float, Int, Float): String` | `nativeGenerate` | `(Ljava/lang/String;IFFFF)Ljava/lang/String;` | ✓ |
| `nativeTokenize(String): IntArray?` | `nativeTokenize` | `(Ljava/lang/String;)[I` | ✓ |
| `nativeApplyLora(String): Boolean` | `nativeApplyLora` | `(Ljava/lang/String;)Z` | ✓ |
| `nativeIsLoaded(): Boolean` | `nativeIsLoaded` | `()Z` | ✓ |

All signatures match. No mismatch risk.

---

## B2. Threading Model

### CRITICAL FINDING: Single-thread dispatcher is NOT enforced

The architecture doc explicitly warns in Gotcha G4:

> *"Create a dedicated single-thread dispatcher (`newSingleThreadContext("llama-inference")`) and use it for ALL inference calls. Never call `llama_generate` from multiple coroutines concurrently on the same model."*

**Actual code** (`InferenceRepositoryImpl.kt` line 36):
```kotlin
private val inferenceDispatcher = Dispatchers.Default  // THIS IS A THREAD POOL
```

**Why this is dangerous:** `Dispatchers.Default` is backed by `kotlinx.coroutines.scheduling.DefaultScheduler`, which maintains a pool of `max(2, CPU cores)` threads. On an 8-core phone, that's 8 threads. If two coroutines call `generate()` simultaneously:

1. Coroutine A starts calling `llama_decode` on thread T1
2. Coroutine B starts calling `llama_decode` on thread T2
3. Both operate on the same `g_context` (global static in llama_jni.cpp line 22)
4. llama.cpp's context state is corrupted → undefined behavior, likely SIGSEGV

**CoroutineDispatchers.kt also fails** (line 12):
```kotlin
val Inference: CoroutineDispatcher get() = Dispatchers.Default  // Same bug
```

The comment says "Overridden per-platform" but there's no platform override — `CoroutineDispatchers` is in commonMain with no expect/actual for dispatchers.

### Additional threading issues:

| Issue | File | Lines |
|-------|------|-------|
| `HomeViewModel.generateInsight()` uses `viewModelScope.launch` (Main dispatcher) but the actual inference runs on InferenceRepository's dispatcher — correct delegation. | `HomeViewModel.kt` | 59-76 |
| `ScreenDataRepositoryImpl` collects from SharedFlow on `Dispatchers.Default` scope — correct, since SharedFlow is thread-safe. | `ScreenDataRepositoryImpl.kt` | 36-46 |
| `ModelDownloadDataSource` uses `withContext(Dispatchers.IO)` for HTTP — correct for IO-bound work. | `ModelDownloadDataSource.kt` | 75 |
| `ScreenReaderBridge.sendData()` is `suspend` and called from AccessibilityService callback (main thread) — but `SharedFlow.emit()` is suspend and will block the callback thread if buffer is full (64 elements). This could delay AccessibilityService response. Should use `tryEmit` or launch into a coroutine. | `FinanceScreenReaderService.kt` | 153, `ScreenReaderBridge.kt` | 29-30 |
| **Bug:** `ScreenReaderBridge.sendData()` is a `suspend fun` called from `scope.launch` in FinanceScreenReaderService, so it won't block the accessibility callback. OK. | | |

---

## B3. Error Handling

### What's handled:

1. **Download errors:** `ModelDownloadDataSource` has try/catch for HTTP errors, cancellation, and IO exceptions (lines 74-191).
2. **Inference errors:** `InferenceRepositoryImpl` wraps the entire inference flow in try/catch/finally (lines 63-127), ensuring model is unloaded.
3. **Catalog load:** `FinanceSlmApp.loadModelCatalog()` catches exceptions silently (lines 41-43).

### What's NOT handled:

| Scenario | Current behavior | Risk |
|----------|-----------------|------|
| **Native crash (SIGSEGV) during inference** | App crashes with no recovery. The Kotlin `try/catch` block cannot catch native crashes. | High |
| **Model file corrupted mid-inference** | Same — native crash. | High |
| **Model download interrupted by app kill** | Partial file left on disk. On next launch, `ModelDownloadDataSource` will try SHA256 verification (which fails), then delete and restart. This works but the partial file wastes space until next launch. | Medium |
| **Out of memory during model load** | `llama_model_load_from_file` returns null, which is handled (line 62-65). But if the OOM happens in the allocator, it could be a native crash. | Medium |
| **Network timeout during download** | `connectTimeout = 30000, readTimeout = 60000` (lines 78-79). If the network drops, it throws IOException → caught → `DownloadState.Error`. Good. | Low |
| **`generateFromLatestScreen` with no model selected** | `error("No model selected")` — crashes the coroutine, caught by ViewModel's catch block, shows error in UI. Acceptable for now. | Low |
| **`generateFromLatestScreen` with no screen data** | `error("No screen data available")` — same pattern. Acceptable. | Low |

### Recommendation:
Add a JNI-level signal handler (SIGSEGV/SIGABRT catcher) that converts native crashes to Java exceptions, or at minimum logs the crash before the process dies. Without this, a bad model file or OOM condition silently kills the app.

---

## B4. Memory Management

### Model/context lifecycle:

```
loadModel(path) → llama_backend_init → llama_model_load_from_file → llama_init_from_model
                                                                           ↓
unloadModel()  ← llama_model_free          ←          llama_free
```

**Issue 1: llama_backend_free never called.** `llama_backend_init()` allocates backend resources (BLAS handles, GPU memory if applicable). These are never freed — even after `freeModel()`. Add a `JNI_OnUnload` handler.

**Issue 2: LoRA adapter ownership ambiguity.** In `llama_jni.cpp` line 306:
```cpp
// NOTE: adapter remains valid as long as the model is alive.
// We don't free it here since llama_set_adapters_lora takes ownership.
```
This is technically correct — `llama_set_adapters_lora` takes ownership of the adapter array. But there's no way to REMOVE a LoRA adapter once applied. The adapter persists until the context is freed. If a user switches between LoRA adapters, old adapters accumulate in the context's adapter list.

**Issue 3: `libllamajni.so` loaded once, never unloaded.** The companion object in `LlamaEngineAndroid` (line 48-57) loads the library once via `System.loadLibrary`. There's no mechanism to unload it. This is fine for normal operation — Android never unloads native libraries from a running app process. But there's no native-level cleanup on app destroy.

**Issue 4: Inference output buffer growth.** The C++ `nativeGenerate` uses `output.reserve(maxTokens * 4)` (line 187) which is a reasonable estimate of ~4 bytes per token. No unbounded growth risk.

**Issue 5: No memory pressure handling.** On low-memory devices (4GB RAM), loading a 3B Q4_K_M model (~1.9GB file) could trigger Android's `onTrimMemory()`. The app has no handler for this — it should unload the model and disable inference features when memory is low.

---

## B5. AccessibilityService Lifecycle

### Service declaration (AndroidManifest.xml lines 27-37):
```xml
<service
    android:name=".service.FinanceScreenReaderService"
    android:exported="true"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
```

Correct — the `BIND_ACCESSIBILITY_SERVICE` permission ensures only the system can bind to it.

### Configuration (accessibility_service_config.xml):
```xml
<accessibility-service
    android:accessibilityEventTypes="typeWindowContentChanged|typeWindowStateChanged|typeNotificationStateChanged"
    android:canRetrieveWindowContent="true"
    android:notificationTimeout="500"
    ... />
```

**Note:** `canRetrieveWindowContent` is NOT set in the XML. It IS set programmatically at line 99 of the service (`serviceInfo = AccessibilityServiceInfo().apply { ... }`), but the XML also needs it for system-level awareness. Actually, looking at the API — `canRetrieveWindowContent` CAN be set programmatically in `onServiceConnected`. The XML `description` references `@string/accessibility_service_description` — this must exist in `res/values/strings.xml` or the XML won't compile.

### Foreground service notification:
The service promotes itself to foreground (line 116: `startForegroundService()`) with a low-importance notification. This is required on Android 8+ for accessibility services that run continuously. The notification is correctly set to silent (line 323: `setSilent(true)`) and ongoing.

### Lifecycle correctness:

| Event | Handler | Correct? |
|-------|---------|----------|
| Service connected | `onServiceConnected` → configures serviceInfo, creates notification channel, starts foreground | ✓ |
| Accessibility event | `onAccessibilityEvent` → filters by package, extracts text on coroutine, sends via bridge | ✓ (see below) |
| Service interrupted | `onInterrupt` → logs only | ⚠️ Should restart or notify user |
| Service destroyed | `onDestroy` → logs only | ⚠️ Should clean up coroutine scope |

**Bug in onDestroy:** The `scope` (`CoroutineScope(SupervisorJob() + Dispatchers.Default)` at line 93) is never cancelled on `onDestroy`. Any in-flight text collection coroutines will continue running after the service is destroyed, potentially accessing recycled AccessibilityNodeInfo objects.

**Fix:**
```kotlin
override fun onDestroy() {
    scope.cancel()  // Cancel all in-flight coroutines
    super.onDestroy()
}
```

---

# Part C: Enhancement Proposals

## Priority Summary

| # | Enhancement | Priority | Complexity |
|---|------------|----------|------------|
| E1 | Fix single-thread inference dispatcher | **P0** | S |
| E2 | Move ModelDownloadDataSource to androidMain + add Ktor expect/actual | **P0** | M |
| E3 | Implement real token streaming | **P1** | L |
| E4 | Add persistent insight storage (SQLDelight) | **P1** | L |
| E5 | Fix JNI crash handling + add crash reporting | **P1** | M |
| E6 | Implement model switch/unload logic | **P1** | S |
| E7 | Fix LoraEditorScreen + LoRA persistence | **P1** | S |
| E8 | Add model-specific chat templates | **P2** | M |
| E9 | Improve AccessibilityService robustness | **P2** | S |
| E10 | Add download foreground service / WorkManager | **P2** | M |

---

### E1: Fix Single-Thread Inference Dispatcher

- **Priority:** P0 (Critical)
- **Complexity:** S (2 files, ~15 lines changed)
- **Risk:** Without this, concurrent inference calls cause undefined behavior and native crashes.

**Description:**
Replace `Dispatchers.Default` with a dedicated `newSingleThreadContext("llama-inference")` in `InferenceRepositoryImpl` and ensure ALL llama.cpp calls go through this dispatcher.

**Files to change:**
1. `shared/src/commonMain/kotlin/com/habibi/financeslm/data/repository/InferenceRepositoryImpl.kt` (line 36)
2. `shared/src/androidMain/kotlin/com/habibi/financeslm/inference/LlamaEngine.android.kt` (lines 61, 76, 87, 105, 110)

**Approach:**
1. Create `SingleThreadInferenceDispatcher` in a new file `shared/src/commonMain/kotlin/com/habibi/financeslm/util/InferenceDispatcher.kt`:
   ```kotlin
   object InferenceDispatcher {
       val dispatcher: CoroutineDispatcher = 
           newSingleThreadContext("llama-inference").asCoroutineDispatcher() // KMP-safe
   }
   ```
   (Note: `newSingleThreadContext` is JVM-only. For KMP, use `Executors.newSingleThreadExecutor().asCoroutineDispatcher()` — this is available via `kotlinx-coroutines-core`.)

2. Replace all `Dispatchers.Default` usage in llama.cpp call paths with `InferenceDispatcher.dispatcher`.

3. Add cleanup in a teardown function (call from Application.onTerminate or similar):
   ```kotlin
   fun shutdown() {
       (dispatcher as ExecutorCoroutineDispatcher).close()
   }
   ```

---

### E2: Move ModelDownloadDataSource to androidMain + Ktor expect/actual

- **Priority:** P0 (Critical)
- **Complexity:** M (5+ files, significant refactor)
- **Risk:** Current code won't compile for iOS targets.

**Description:**
The `ModelDownloadDataSource` currently uses `java.net.HttpURLConnection` in `commonMain`, which is a JVM-only API. Create an `expect`/`actual` interface for model downloading, with the current implementation moving to `androidMain` and a Ktor-based or platform HTTP implementation behind an interface.

**Files to change:**
1. `shared/src/commonMain/kotlin/com/habibi/financeslm/data/datasource/ModelDownloadDataSource.kt` → convert to `expect` interface
2. NEW: `shared/src/androidMain/kotlin/com/habibi/financeslm/data/datasource/ModelDownloadDataSource.android.kt` — current HttpURLConnection implementation
3. NEW: `shared/src/iosMain/kotlin/com/habibi/financeslm/data/datasource/ModelDownloadDataSource.ios.kt` — Ktor-based or NSURLSession implementation
4. `shared/src/commonMain/kotlin/com/habibi/financeslm/data/repository/ModelRepositoryImpl.kt` — should not need changes if interface stays the same
5. `androidApp/src/main/java/com/habibi/financeslm/android/di/AndroidAppModule.kt` — wire platform-specific download data source

**Alternative (simpler):**
If iOS isn't being built yet, just move the file to `androidMain` and add a TODO for iOS. This unblocks the build integrity concern without full Ktor migration.

---

### E3: Implement Real Token Streaming

- **Priority:** P1 (High)
- **Complexity:** L (JNI changes + Kotlin changes)
- **Risk:** Current UX shows nothing for 5-30 seconds during generation.

**Description:**
Modify the JNI bridge to support callback-based streaming. Instead of returning the full generated text from `nativeGenerate`, provide a JNI callback that emits tokens as they're generated.

**Files to change:**
1. `shared/src/androidMain/cpp/llama_jni.cpp` — new `nativeGenerateStreaming` function with JNI callback
2. `shared/src/androidMain/kotlin/com/habibi/financeslm/inference/LlamaEngine.android.kt` — new `inferStreaming` method
3. `shared/src/commonMain/kotlin/com/habibi/financeslm/inference/LlamaEngine.kt` — add `inferStreaming` to interface
4. `shared/src/commonMain/kotlin/com/habibi/financeslm/data/repository/InferenceRepositoryImpl.kt` — use streaming variant
5. `androidApp/src/main/java/com/habibi/financeslm/android/ui/viewmodel/HomeViewModel.kt` — already has `generationOutput` state that appends tokens (line 68)

**Approach:**
```cpp
// New JNI function that accepts a Java Consumer<String> callback
extern "C" void JNICALL
Java_..._nativeGenerateStreaming(
    JNIEnv * env, jobject thiz,
    jstring prompt, jint maxTokens, ..., jobject callback)
{
    // In the token generation loop, call callback.accept(tokenPiece) for each token
}
```

The Kotlin side wraps the callback into a `callbackFlow<String>` that truly streams token-by-token.

---

### E4: Add Persistent Insight Storage (SQLDelight)

- **Priority:** P1 (High)
- **Complexity:** L (schema + migration + repository refactor)
- **Risk:** All user-generated insights are lost on app restart.

**Description:**
Wire up SQLDelight for persisting `FinanceInsight` and `ScreenData` objects. The architecture doc specifies SQLDelight in the dependency list but it's not used in the current implementation.

**Files to change:**
1. NEW: `shared/src/commonMain/sqldelight/com/habibi/financeslm/FinanceInsight.sq` — schema
2. NEW: `shared/src/commonMain/sqldelight/com/habibi/financeslm/ScreenData.sq` — schema
3. `shared/src/commonMain/kotlin/com/habibi/financeslm/data/repository/InferenceRepositoryImpl.kt` — persist to DB instead of in-memory
4. `shared/src/androidMain/.../ScreenDataRepositoryImpl.android.kt` — persist to DB
5. `shared/build.gradle.kts` — add SQLDelight plugin configuration
6. `shared/src/androidMain/.../PlatformModule.android.kt` (or DI module) — provide `SqlDriver`

---

### E5: Fix JNI Crash Handling + Crash Reporting

- **Priority:** P1 (High)
- **Complexity:** M
- **Risk:** Native crashes in llama.cpp silently kill the app with no user feedback or diagnostics.

**Description:**
Add a JNI-level signal handler for SIGSEGV/SIGABRT that converts fatal native signals into Java exceptions, logs the crash, and optionally integrates with Firebase Crashlytics for native crash reporting.

**Files to change:**
1. `shared/src/androidMain/cpp/llama_jni.cpp` — add signal handler in JNI_OnLoad
2. NEW: `shared/src/androidMain/cpp/crash_handler.cpp` — signal handler implementation
3. `shared/src/commonMain/kotlin/com/habibi/financeslm/data/repository/InferenceRepositoryImpl.kt` — catch the converted exception
4. `androidApp/build.gradle.kts` — add Firebase Crashlytics NDK dependency (if opting for reporting)

**Signal handler approach:**
```cpp
#include <signal.h>
#include <setjmp.h>

static jmp_buf crash_jmp_buf;

static void crash_signal_handler(int sig) {
    // Log the crash
    __android_log_print(ANDROID_LOG_ERROR, "LlamaEngine", 
        "Native crash: signal %d", sig);
    longjmp(crash_jmp_buf, 1);
}

// In JNI load: signal(SIGSEGV, crash_signal_handler);
// In generate: if (setjmp(crash_jmp_buf)) { throw java exception; }
```

**Note:** `setjmp`/`longjmp` is fragile and not recommended in production. A better approach is to guard the JNI call with a try/catch via a helper that invokes `nativeGenerate` in a separate process or isolates memory. For v1, using `Breakpad` or `Google Crashpad` for crash reporting is more practical than crash recovery.

---

### E6: Implement Model Switch/Unload Logic

- **Priority:** P1 (High)
- **Complexity:** S
- **Risk:** Memory leak — loaded models are never unloaded except during inference cleanup. Switching models leaves old model in memory.

**Description:**
The `InferenceRepositoryImpl` load/unload happens per-`generate()` call (load → infer → unload). This means every insight generation re-loads the model from disk (slow). The architecture should:
1. Keep the model loaded between generations (with a configurable idle timeout)
2. Provide explicit `unloadActiveModel()` for when user switches models or app goes to background
3. Handle Android lifecycle: unload in `onTrimMemory()`, reload on resume

**Files to change:**
1. `shared/src/commonMain/kotlin/com/habibi/financeslm/data/repository/InferenceRepositoryImpl.kt` — add `loadModelOnce()`, `unloadModel()`, idle timeout
2. `androidApp/src/main/java/com/habibi/financeslm/android/FinanceSlmApp.kt` — add `onTrimMemory` handler
3. `androidApp/src/main/java/com/habibi/financeslm/android/ui/viewmodel/HomeViewModel.kt` — call load/unload lifecycle

---

### E7: Fix LoraEditorScreen + LoRA Persistence

- **Priority:** P1 (High)
- **Complexity:** S
- **Risk:** LoRA editing is completely non-functional (dead UI).

**Description:**
Wire up `LoraEditorViewModel` to the composable fields, add actual save/create/delete logic, and connect to the `ManageLoraUseCase`.

**Files to change:**
1. `androidApp/src/main/java/com/habibi/financeslm/android/ui/screens/settings/ModelManagementScreen.kt` — lines 285-363 (LoraEditorScreen composable)
2. `androidApp/src/main/java/com/habibi/financeslm/android/ui/viewmodel/LoraEditorViewModel.kt` — verify save/create/delete functions exist
3. `androidApp/src/main/java/com/habibi/financeslm/android/ui/navigation/AppNavGraph.kt` — ensure LoraEditorScreen receives LoraEditorViewModel

**Approach:**
- Replace hardcoded `value = ""` with `loraEditorViewModel.name` and `loraEditorViewModel.instructionText`
- Replace `onValueChange = {}` with actual state setters
- Wire `Button(onClick = onBack)` to call `loraEditorViewModel.save()` first, then navigate back
- Wire delete button to call `loraEditorViewModel.delete()` with confirmation dialog

---

### E8: Add Model-Specific Chat Templates

- **Priority:** P2 (Medium)
- **Complexity:** M
- **Risk:** Non-Qwen models produce garbled output.

**Description:**
The `PromptTemplate` hardcodes `<|im_start|>` tokens (Qwen format). Add per-model chat template configuration, either:
- Extending `model_catalog.json` with a `chatTemplate` field
- Auto-detecting from GGUF metadata (llama.cpp API provides `llama_chat_template()`)

**Files to change:**
1. `androidApp/src/main/assets/model_catalog.json` — add `chatTemplate` field per model
2. `shared/src/commonMain/kotlin/com/habibi/financeslm/prompt/PromptTemplate.kt` — accept template format parameter
3. `shared/src/commonMain/kotlin/com/habibi/financeslm/prompt/PromptBuilder.kt` — pass template from model
4. `shared/src/commonMain/kotlin/com/habibi/financeslm/domain/model/ModelInfo.kt` — add `chatTemplate` field

**Template mappings:**
```json
{
    "qwen": "<|im_start|>system\n{system}<|im_end|>\n<|im_start|>user\n{user}<|im_end|>\n<|im_start|>assistant\n",
    "llama": "<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n\n{system}<|eot_id|><|start_header_id|>user<|end_header_id|>\n\n{user}<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n\n",
    "mistral": "[INST] {system}\n\n{user} [/INST]"
}
```

---

### E9: Improve AccessibilityService Robustness

- **Priority:** P2 (Medium)
- **Complexity:** S
- **Risk:** StackOverflowError on deeply nested UIs; coroutine leak on destroy; OTP notification capture.

**Description:**
Three targeted fixes to the AccessibilityService:

1. **Iterative tree traversal** (replace recursion)
2. **Cancel coroutine scope on destroy**
3. **Filter notification events to exclude OTPs**

**Files to change:**
1. `androidApp/src/main/java/com/habibi/financeslm/android/service/FinanceScreenReaderService.kt`

**Changes:**
```kotlin
// 1. Iterative traversal
private fun collectText(root: AccessibilityNodeInfo, results: MutableList<String>) {
    val stack = ArrayDeque<AccessibilityNodeInfo>()
    stack.add(root)
    while (stack.isNotEmpty()) {
        val node = stack.removeFirst()
        if (node.isPassword) { node.recycle(); continue }
        // ... extract text ...
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { stack.add(it) }
        }
        node.recycle()
    }
}

// 2. Fix onDestroy
override fun onDestroy() {
    scope.cancel()
    super.onDestroy()
}

// 3. Filter notifications
override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    if (event?.eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
        return // Skip notifications — they may contain OTPs
    }
    // ... rest of handler
}
```

---

### E10: Add Download Foreground Service / WorkManager

- **Priority:** P2 (Medium)
- **Complexity:** M
- **Risk:** Large model downloads (400MB-1.9GB) are interrupted if app is backgrounded or killed.

**Description:**
The architecture doc's Gotcha G5 recommends using `WorkManager` for model downloads so they survive process death. Currently, downloads run in a `callbackFlow` coroutine that dies with the process.

**Files to change:**
1. NEW: `androidApp/src/main/java/com/habibi/financeslm/android/service/ModelDownloadWorker.kt` — WorkManager worker
2. `androidApp/src/main/java/com/habibi/financeslm/android/di/AndroidAppModule.kt` — provide WorkManager
3. `shared/src/commonMain/kotlin/com/habibi/financeslm/data/repository/ModelRepositoryImpl.kt` — delegate download to platform service
4. `shared/src/commonMain/kotlin/com/habibi/financeslm/data/datasource/ModelDownloadDataSource.kt` — add notification progress callbacks

**Alternative (simpler):** Use a foreground service with an ongoing notification showing download progress, without WorkManager. This keeps the app process alive while the download is active. The notification displays "Downloading Finance SLM model... 45%".

---

## Appendix: Codebase Statistics

| Metric | Count |
|--------|-------|
| Kotlin files (project source only) | 81 |
| C++ files (JNI) | 2 |
| Total Kotlin LOC | ~4,876 |
| JNI C++ LOC | 378 |
| Shared module files | ~50 |
| Android app files | ~31 |
| iOS files | ~7 (stubs) |
| Phases completed | 7 of 7 |

---

*End of review. Prepared for Suresh to execute against. Prioritize P0 items first (E1, E2), then P1 items (E3-E7) in listed order.*

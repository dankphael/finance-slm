// crash_handler.cpp — Signal handlers for native crashes (SIGSEGV, SIGABRT)
//
// Installs signal handlers that log crash details and attempt to convert
// native crashes to Java exceptions so the Kotlin layer can catch them
// gracefully instead of the app being silently killed.
//
// NOTE: Signal handlers run in a restricted context. Throwing Java
// exceptions from within a SIGSEGV handler is best-effort — if the
// heap is corrupted, the exception throw will fail and we fall back
// to the default signal behavior (abort + dump).

#include "crash_handler.h"

#include <android/log.h>
#include <cstdio>
#include <signal.h>
#include <string.h>
#include <unistd.h>

#define LOG_TAG "CrashHandler"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// ── Saved state ───────────────────────────────────────────────────────────
// We need a JavaVM reference to throw exceptions from signal context.
static JavaVM * g_jvm = nullptr;

// ── Signal handler ────────────────────────────────────────────────────────
// Called when SIGSEGV or SIGABRT is raised.
// Attempts to log the crash and throw a Java RuntimeException.
// Falls back to the default handler if the exception throw fails.
static void handleNativeCrash(int sig, siginfo_t * info, void * /*ucontext*/) {
    // 1. Log the crash (async-signal-safe: write() to stderr + __android_log_print)
    const char * sigName;
    switch (sig) {
        case SIGSEGV: sigName = "SIGSEGV"; break;
        case SIGABRT: sigName = "SIGABRT"; break;
        default:      sigName = "UNKNOWN"; break;
    }

    LOGE("============================================");
    LOGE("NATIVE CRASH: %s", sigName);
    if (info) {
        LOGE("Fault address: %p", info->si_addr);
        LOGE("Signal code: %d", info->si_code);
    }
    LOGE("============================================");

    // 2. Try to throw a Java exception back to Kotlin
    if (g_jvm) {
        JNIEnv * env = nullptr;
        jint getEnvResult = g_jvm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6);
        if (getEnvResult == JNI_OK && env) {
            char buf[256];
            snprintf(buf, sizeof(buf),
                     "Native crash: %s at address %p. The llama.cpp inference engine "
                     "encountered a fatal error. Please try again with a different model.",
                     sigName,
                     info ? info->si_addr : nullptr);

            jclass runtimeException = env->FindClass("java/lang/RuntimeException");
            if (runtimeException) {
                env->ThrowNew(runtimeException, buf);
                // If we get here, the exception was successfully thrown.
                // Return to let the caller handle the pending exception.
                env->DeleteLocalRef(runtimeException);
                LOGI("CrashHandler: RuntimeException thrown successfully");
                return;
            }
        }
    }

    // 3. Fallback — restore default handler and re-raise
    LOGE("CrashHandler: falling back to default handler");
    struct sigaction sa;
    memset(&sa, 0, sizeof(sa));
    sa.sa_handler = SIG_DFL;
    sigaction(sig, &sa, nullptr);
    raise(sig);
}

// ── Public API ────────────────────────────────────────────────────────────

void installCrashHandler(JNIEnv * env) {
    // Store JavaVM reference for use in signal handler
    if (env) {
        env->GetJavaVM(&g_jvm);
    }

    struct sigaction sa;
    memset(&sa, 0, sizeof(sa));
    sa.sa_sigaction = handleNativeCrash;
    sa.sa_flags = SA_SIGINFO | SA_NODEFER;

    // Install handler for SIGSEGV (segmentation fault)
    if (sigaction(SIGSEGV, &sa, nullptr) != 0) {
        LOGE("Failed to install SIGSEGV handler");
    } else {
        LOGI("SIGSEGV handler installed");
    }

    // Install handler for SIGABRT (assertion failure / abort())
    if (sigaction(SIGABRT, &sa, nullptr) != 0) {
        LOGE("Failed to install SIGABRT handler");
    } else {
        LOGI("SIGABRT handler installed");
    }
}

void uninstallCrashHandler() {
    struct sigaction sa;
    memset(&sa, 0, sizeof(sa));
    sa.sa_handler = SIG_DFL;

    sigaction(SIGSEGV, &sa, nullptr);
    sigaction(SIGABRT, &sa, nullptr);
    g_jvm = nullptr;

    LOGI("Crash handlers uninstalled");
}
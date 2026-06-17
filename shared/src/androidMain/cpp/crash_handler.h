// crash_handler.h — Signal handlers for native crashes (SIGSEGV, SIGABRT)
//
// Installs signal handlers that log crash details and convert native crashes
// to Java exceptions so the Kotlin layer can catch them gracefully instead of
// the app being silently killed.
//
// Usage: call installCrashHandler(env) in JNI_OnLoad.
//        Call uninstallCrashHandler() in JNI_OnUnload.

#ifndef CRASH_HANDLER_H
#define CRASH_HANDLER_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Install signal handlers for SIGSEGV and SIGABRT.
 * Must be called from JNI_OnLoad after a JNIEnv is available.
 * The handler logs the crash via __android_log_print and throws a
 * Java RuntimeException with the crash signal information.
 */
void installCrashHandler(JNIEnv * env);

/**
 * Uninstall signal handlers and restore defaults.
 * Called from JNI_OnUnload to avoid lingering handlers after library unload.
 */
void uninstallCrashHandler();

#ifdef __cplusplus
}
#endif

#endif // CRASH_HANDLER_H
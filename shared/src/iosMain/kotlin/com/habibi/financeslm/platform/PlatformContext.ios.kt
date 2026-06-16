package com.habibi.financeslm.platform

/**
 * iOS PlatformContext — wraps NSObject/application context. Stub for compilation.
 */
actual class PlatformContext {
    actual val filesDir: String = "/var/mobile/Containers/Data/Application/UNKNOWN/Documents"
    actual val cacheDir: String = "/tmp"

    companion object {
        private var instance: PlatformContext? = null
        fun init(): PlatformContext = PlatformContext().also { instance = it }
        fun getInstance(): PlatformContext = instance ?: PlatformContext()
    }
}

actual fun createPlatformContext(): PlatformContext = PlatformContext.getInstance()
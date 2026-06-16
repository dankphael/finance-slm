package com.habibi.financeslm.platform

/**
 * Android PlatformContext — wraps android.content.Context.
 */
actual class PlatformContext {
    actual val filesDir: String = "/data/data/com.habibi.financeslm/files"
    actual val cacheDir: String = "/data/data/com.habibi.financeslm/cache"

    internal lateinit var androidContext: android.content.Context

    companion object {
        private var instance: PlatformContext? = null

        fun init(context: android.content.Context): PlatformContext {
            return PlatformContext().also {
                it.androidContext = context
                instance = it
            }
        }

        fun getInstance(): PlatformContext {
            return instance ?: error("PlatformContext not initialized. Call PlatformContext.init() from Application.onCreate()")
        }
    }
}

actual fun createPlatformContext(): PlatformContext = PlatformContext.getInstance()
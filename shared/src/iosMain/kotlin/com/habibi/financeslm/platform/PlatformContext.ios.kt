package com.habibi.financeslm.platform

import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

/**
 * iOS PlatformContext — resolves the app sandbox's Documents and Caches
 * directories via Foundation. Self-initializing: unlike Android (which needs the
 * Application context), iOS can resolve these paths without any external handle,
 * so Swift does not need to call an init.
 */
actual class PlatformContext {
    actual val filesDir: String = iosDirectory(NSDocumentDirectory)
        ?: "${NSHomeDirectoryFallback()}/Documents"
    actual val cacheDir: String = iosDirectory(NSCachesDirectory)
        ?: "${NSHomeDirectoryFallback()}/Library/Caches"

    companion object {
        private var instance: PlatformContext? = null
        fun init(): PlatformContext = PlatformContext().also { instance = it }
        fun getInstance(): PlatformContext = instance ?: PlatformContext().also { instance = it }
    }
}

private fun iosDirectory(directory: NSSearchPathDirectory): String? {
    val paths = NSSearchPathForDirectoriesInDomains(directory, NSUserDomainMask, true)
    return paths.firstOrNull() as? String
}

private fun NSHomeDirectoryFallback(): String =
    platform.Foundation.NSHomeDirectory()

actual fun createPlatformContext(): PlatformContext = PlatformContext.getInstance()

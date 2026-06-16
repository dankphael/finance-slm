package com.habibi.financeslm.platform

/**
 * iOS FileSystem — uses iOS directory APIs. Stub for compilation.
 */
actual class FileSystem {
    private var context: PlatformContext? = null

    fun init(ctx: PlatformContext) {
        context = ctx
    }

    actual fun getModelsDir(): String = "${context?.filesDir ?: "/var/mobile/Documents"}/models"
    actual fun getLoraDir(): String = "${context?.filesDir ?: "/var/mobile/Documents"}/lora"
    actual fun getCacheDir(): String = context?.cacheDir ?: "/tmp"
    actual fun getDataDir(): String = context?.filesDir ?: "/var/mobile/Documents"

    actual fun fileExists(path: String): Boolean = false
    actual fun deleteFile(path: String): Boolean = false
    actual fun getAvailableSpace(path: String): Long = 4L * 1024 * 1024 * 1024
}

actual fun createFileSystem(platformContext: PlatformContext): FileSystem =
    FileSystem().also { it.init(platformContext) }
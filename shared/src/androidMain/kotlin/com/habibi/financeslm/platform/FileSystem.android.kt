package com.habibi.financeslm.platform

import com.habibi.financeslm.util.Logger
import java.io.File

/**
 * Android FileSystem — uses android.content.Context to resolve directories.
 */
actual class FileSystem {
    private var context: PlatformContext? = null

    fun init(ctx: PlatformContext) {
        context = ctx
    }

    actual fun getModelsDir(): String = "${context?.filesDir ?: "/data/data/com.habibi.financeslm/files"}/models"
    actual fun getLoraDir(): String = "${context?.filesDir ?: "/data/data/com.habibi.financeslm/files"}/lora"
    actual fun getCacheDir(): String = context?.cacheDir ?: "/data/data/com.habibi.financeslm/cache"
    actual fun getDataDir(): String = context?.filesDir ?: "/data/data/com.habibi.financeslm/files"

    actual fun fileExists(path: String): Boolean = File(path).exists()
    actual fun deleteFile(path: String): Boolean = File(path).delete()

    actual fun getAvailableSpace(path: String): Long {
        val statFs = android.os.StatFs(path)
        return statFs.availableBlocksLong * statFs.blockSizeLong
    }
}

actual fun createFileSystem(platformContext: PlatformContext): FileSystem =
    FileSystem().also { it.init(platformContext) }
package com.habibi.financeslm.platform

import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSystemFreeSize
import platform.Foundation.NSNumber
import okio.FileSystem as OkioFileSystem
import okio.Path.Companion.toPath

/**
 * iOS FileSystem — real implementation backed by okio's [OkioFileSystem.SYSTEM]
 * (cross-platform file ops) plus Foundation for free-space queries. Directory
 * roots come from [PlatformContext] (the app sandbox's Documents/Caches).
 */
actual class FileSystem {
    private var context: PlatformContext = PlatformContext.getInstance()

    fun init(ctx: PlatformContext) {
        context = ctx
    }

    private val okio: OkioFileSystem get() = OkioFileSystem.SYSTEM

    actual fun getModelsDir(): String = ensureDir("${context.filesDir}/models")
    actual fun getLoraDir(): String = ensureDir("${context.filesDir}/lora")
    actual fun getCacheDir(): String = context.cacheDir
    actual fun getDataDir(): String = context.filesDir

    actual fun fileExists(path: String): Boolean = try {
        okio.exists(path.toPath())
    } catch (e: Exception) {
        false
    }

    actual fun deleteFile(path: String): Boolean = try {
        val p = path.toPath()
        if (okio.exists(p)) {
            okio.delete(p)
            true
        } else {
            false
        }
    } catch (e: Exception) {
        false
    }

    actual fun getAvailableSpace(path: String): Long = try {
        val attrs = NSFileManager.defaultManager.attributesOfFileSystemForPath(path, null)
        (attrs?.get(NSFileSystemFreeSize) as? NSNumber)?.longLongValue ?: -1L
    } catch (e: Exception) {
        -1L
    }

    private fun ensureDir(dir: String): String {
        try {
            okio.createDirectories(dir.toPath())
        } catch (_: Exception) {
            // best-effort
        }
        return dir
    }
}

actual fun createFileSystem(platformContext: PlatformContext): FileSystem =
    FileSystem().also { it.init(platformContext) }

package com.habibi.financeslm.platform

/**
 * FileSystem — provides platform-specific directory paths for models, LoRA files, etc.
 */
expect class FileSystem {
    fun getModelsDir(): String
    fun getLoraDir(): String
    fun getCacheDir(): String
    fun getDataDir(): String
    fun fileExists(path: String): Boolean
    fun deleteFile(path: String): Boolean
    fun getAvailableSpace(path: String): Long
}

expect fun createFileSystem(platformContext: PlatformContext): FileSystem
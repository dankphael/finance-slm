package com.habibi.financeslm.util

import okio.FileSystem as OkioFileSystem
import okio.HashingSink
import okio.Path.Companion.toPath
import okio.blackholeSink
import okio.buffer
import okio.use

/**
 * iOS ChecksumVerifier — computes a real SHA-256 over the file using okio's
 * multiplatform [HashingSink] and compares it (case-insensitively) to the
 * expected digest. An empty [expectedSha256] is treated as "no verification".
 */
actual class ChecksumVerifier {
    actual suspend fun verify(filePath: String, expectedSha256: String): Boolean {
        if (expectedSha256.isEmpty()) return true
        return try {
            val path = filePath.toPath()
            val fs = OkioFileSystem.SYSTEM
            if (!fs.exists(path)) {
                Logger.w("ChecksumVerifier", "File not found: $filePath")
                return false
            }
            val hashingSink = HashingSink.sha256(blackholeSink())
            fs.source(path).buffer().use { source ->
                source.readAll(hashingSink)
            }
            val actual = hashingSink.hash.hex()
            val matches = actual.equals(expectedSha256, ignoreCase = true)
            if (!matches) {
                Logger.w("ChecksumVerifier", "SHA256 mismatch: expected=$expectedSha256 actual=$actual")
            }
            matches
        } catch (e: Exception) {
            Logger.e("ChecksumVerifier", "verify failed: ${e.message}")
            false
        }
    }
}

actual fun createChecksumVerifier(): ChecksumVerifier = ChecksumVerifier()

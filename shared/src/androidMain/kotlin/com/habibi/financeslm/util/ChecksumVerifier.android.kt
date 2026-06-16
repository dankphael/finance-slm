package com.habibi.financeslm.util

import java.security.MessageDigest

actual class ChecksumVerifier {
    actual suspend fun verify(filePath: String, expectedSha256: String): Boolean {
        return try {
            val file = java.io.File(filePath)
            if (!file.exists()) return false
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
            actualHash == expectedSha256
        } catch (e: Exception) {
            Logger.e("ChecksumVerifier", "Verification failed", e)
            false
        }
    }
}

actual fun createChecksumVerifier(): ChecksumVerifier = ChecksumVerifier()
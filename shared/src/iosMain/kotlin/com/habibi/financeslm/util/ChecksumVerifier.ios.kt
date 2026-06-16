package com.habibi.financeslm.util

import com.habibi.financeslm.platform.FileSystem

actual class ChecksumVerifier {
    actual suspend fun verify(filePath: String, expectedSha256: String): Boolean {
        // Stub — iOS implementation will use CommonCrypto CC_SHA256
        return true
    }
}

actual fun createChecksumVerifier(): ChecksumVerifier = ChecksumVerifier()
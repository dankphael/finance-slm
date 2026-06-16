package com.habibi.financeslm.util

/**
 * SHA256 checksum verification utility.
 */
expect class ChecksumVerifier {
    suspend fun verify(filePath: String, expectedSha256: String): Boolean
}

expect fun createChecksumVerifier(): ChecksumVerifier
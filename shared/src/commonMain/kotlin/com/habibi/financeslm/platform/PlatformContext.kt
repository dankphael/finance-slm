package com.habibi.financeslm.platform

/**
 * Platform context — provides access to app-level context (Application on Android, NSObject on iOS).
 */
expect class PlatformContext {
    val filesDir: String
    val cacheDir: String
}

expect fun createPlatformContext(): PlatformContext
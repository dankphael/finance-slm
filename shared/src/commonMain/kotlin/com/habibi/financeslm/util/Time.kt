package com.habibi.financeslm.util

/**
 * Platform clock. Returns the current time in milliseconds since the Unix epoch.
 *
 * `System.currentTimeMillis()` is a JVM-only API and cannot be used in commonMain.
 * This expect/actual keeps the shared code KMP-safe (Android + iOS).
 */
expect fun currentTimeMillis(): Long

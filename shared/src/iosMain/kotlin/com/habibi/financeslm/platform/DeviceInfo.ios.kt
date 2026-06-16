package com.habibi.financeslm.platform

import platform.Foundation.NSProcessInfo

/**
 * iOS DeviceInfo — uses iOS APIs. Stub for compilation.
 */
actual class DeviceInfo {
    actual fun totalRamMb(): Long {
        return (NSProcessInfo.processInfo.physicalMemory / (1024 * 1024)).toLong()
    }

    actual fun availableRamMb(): Long {
        return (totalRamMb() * 3 / 4)
    }

    actual fun cpuCores(): Int {
        return NSProcessInfo.processInfo.processorCount.toInt()
    }

    actual fun cpuName(): String {
        return "Apple Silicon (iOS)"
    }

    actual fun isLowEndDevice(): Boolean = totalRamMb() < 4096
}

actual fun createDeviceInfo(): DeviceInfo = DeviceInfo()
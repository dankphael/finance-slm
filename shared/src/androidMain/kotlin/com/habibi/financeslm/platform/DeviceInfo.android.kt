package com.habibi.financeslm.platform

import android.app.ActivityManager
import android.content.Context

/**
 * Android DeviceInfo — uses Android OS APIs to query device hardware.
 */
actual class DeviceInfo {
    private val activityManager: ActivityManager? by lazy {
        val ctx = PlatformContext.getInstance().androidContext
        ctx.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    }

    actual fun totalRamMb(): Long {
        val memInfo = android.app.ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memInfo)
        return memInfo.totalMem / (1024 * 1024)
    }

    actual fun availableRamMb(): Long {
        val memInfo = android.app.ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memInfo)
        return memInfo.availMem / (1024 * 1024)
    }

    actual fun cpuCores(): Int {
        return Runtime.getRuntime().availableProcessors()
    }

    actual fun cpuName(): String {
        return try {
            java.io.BufferedReader(java.io.FileReader("/proc/cpuinfo")).use { reader ->
                reader.readLine() ?: "unknown"
            }
        } catch (e: Exception) {
            "unknown"
        }
    }

    actual fun isLowEndDevice(): Boolean = totalRamMb() < 4096
}

actual fun createDeviceInfo(): DeviceInfo = DeviceInfo()
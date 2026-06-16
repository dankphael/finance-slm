package com.habibi.financeslm.platform

/**
 * DeviceInfo — provides device hardware information for inference tuning.
 */
expect class DeviceInfo {
    fun totalRamMb(): Long
    fun availableRamMb(): Long
    fun cpuCores(): Int
    fun cpuName(): String
    fun isLowEndDevice(): Boolean
}

expect fun createDeviceInfo(): DeviceInfo
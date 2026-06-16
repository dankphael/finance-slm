package com.habibi.financeslm.platform

import com.habibi.financeslm.domain.model.ScreenData
import com.habibi.financeslm.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Android ScreenReader — connects to FinanceScreenReaderService via ScreenReaderBridge.
 * Stub for compilation; real implementation receives data from AccessibilityService.
 */
class AndroidScreenReader : ScreenReader {
    private val _screenData = MutableSharedFlow<ScreenData>()
    private var running = false

    override fun observeScreenData(): Flow<ScreenData> = _screenData.asSharedFlow()

    override suspend fun start() {
        running = true
        Logger.d("ScreenReader", "ScreenReader started (Android)")
    }

    override suspend fun stop() {
        running = false
        Logger.d("ScreenReader", "ScreenReader stopped (Android)")
    }

    override fun isRunning(): Boolean = running
}

actual fun createScreenReader(): ScreenReader = AndroidScreenReader()
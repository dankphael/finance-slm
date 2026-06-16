package com.habibi.financeslm.platform

import com.habibi.financeslm.domain.model.ScreenData
import com.habibi.financeslm.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * iOS ScreenReader — wraps screenshot + OCR pipeline.
 * Stub for compilation.
 */
class IosScreenReader : ScreenReader {
    private val _screenData = MutableSharedFlow<ScreenData>()
    private var running = false

    override fun observeScreenData(): Flow<ScreenData> = _screenData.asSharedFlow()

    override suspend fun start() {
        running = true
        Logger.d("ScreenReader", "ScreenReader started (iOS)")
    }

    override suspend fun stop() {
        running = false
        Logger.d("ScreenReader", "ScreenReader stopped (iOS)")
    }

    override fun isRunning(): Boolean = running
}

actual fun createScreenReader(): ScreenReader = IosScreenReader()
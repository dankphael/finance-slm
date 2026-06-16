package com.habibi.financeslm.platform

import com.habibi.financeslm.domain.model.ScreenData
import kotlinx.coroutines.flow.Flow

/**
 * Screen reader — platform-specific screen data capture.
 * On Android: wraps AccessibilityService.
 * On iOS: wraps screenshot + OCR pipeline.
 */
interface ScreenReader {
    fun observeScreenData(): Flow<ScreenData>
    suspend fun start()
    suspend fun stop()
    fun isRunning(): Boolean
}

expect fun createScreenReader(): ScreenReader
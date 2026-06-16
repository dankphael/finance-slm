package com.habibi.financeslm.service

import com.habibi.financeslm.domain.model.ScreenData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * ScreenReaderBridge — singleton bridge that connects the AccessibilityService process
 * to the main app process via a MutableSharedFlow.
 *
 * Both the AccessibilityService and the main app run in the same process (declared in
 * the same app's AndroidManifest), so a simple in-memory SharedFlow works.
 *
 * Lives in the shared module so both FinanceScreenReaderService (androidApp) and
 * AndroidScreenReader (shared) can access it.
 */
object ScreenReaderBridge {

    private val _screenData = MutableSharedFlow<ScreenData>(
        replay = 0,              // No replay — real-time data only
        extraBufferCapacity = 64 // Buffer up to 64 events before suspending
    )

    /**
     * Called by FinanceScreenReaderService after parsing each accessibility event.
     * Thread-safe — SharedFlow handles concurrent emission.
     */
    suspend fun sendData(screenData: ScreenData) {
        _screenData.emit(screenData)
    }

    /**
     * Called by the main app (via AndroidScreenReader) to observe incoming screen data.
     */
    fun observeData(): Flow<ScreenData> = _screenData.asSharedFlow()
}

package com.habibi.financeslm.data.repository

import com.habibi.financeslm.domain.model.ScreenData
import com.habibi.financeslm.domain.repository.ScreenDataRepository
import com.habibi.financeslm.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Android ScreenDataRepository — wraps AccessibilityService data bridge.
 * Stub for compilation; real implementation connects to FinanceScreenReaderService.
 */
class ScreenDataRepositoryImpl : ScreenDataRepository {
    private val _screenData = MutableSharedFlow<ScreenData>(replay = 10)
    private val _recentData = MutableStateFlow<List<ScreenData>>(emptyList())

    override fun observeScreenData(): Flow<ScreenData> = _screenData.asSharedFlow()

    override suspend fun getRecent(maxCount: Int): List<ScreenData> {
        return _recentData.value.take(maxCount)
    }

    override suspend fun clear() {
        _recentData.value = emptyList()
        Logger.d("ScreenDataRepo", "Cleared screen data")
    }
}
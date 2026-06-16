package com.habibi.financeslm.data.repository

import com.habibi.financeslm.domain.model.ScreenData
import com.habibi.financeslm.domain.repository.ScreenDataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * iOS ScreenDataRepository — receives data from screenshot + OCR pipeline.
 * Stub for compilation.
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
    }
}
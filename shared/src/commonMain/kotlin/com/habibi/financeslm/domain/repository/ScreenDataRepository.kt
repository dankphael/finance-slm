package com.habibi.financeslm.domain.repository

import com.habibi.financeslm.domain.model.ScreenData
import kotlinx.coroutines.flow.Flow

interface ScreenDataRepository {
    fun observeScreenData(): Flow<ScreenData>
    suspend fun getRecent(maxCount: Int = 50): List<ScreenData>
    suspend fun clear()
}
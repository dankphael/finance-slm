package com.habibi.financeslm.data.repository

import com.habibi.financeslm.domain.model.ScreenData
import com.habibi.financeslm.domain.repository.ScreenDataRepository
import com.habibi.financeslm.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Android ScreenDataRepository — connects to AndroidScreenReader (which wraps
 * ScreenReaderBridge) and caches the latest 100 ScreenData events.
 *
 * @param screenDataFlow the source flow of ScreenData from the platform screen reader
 */
class ScreenDataRepositoryImpl(
    private val screenDataFlow: Flow<ScreenData>
) : ScreenDataRepository {

    companion object {
        private const val MAX_CACHED_ENTRIES = 100
    }

    /** Background scope for collecting the source flow. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Cached list of recent screen data entries (most recent first). */
    private val _cache = MutableStateFlow<List<ScreenData>>(emptyList())

    init {
        // Collect from the source flow and maintain the cache
        scope.launch {
            screenDataFlow.collect { data ->
                val current = _cache.value.toMutableList()
                current.add(0, data) // Most recent first
                if (current.size > MAX_CACHED_ENTRIES) {
                    current.removeAt(current.lastIndex)
                }
                _cache.value = current
                Logger.d("ScreenDataRepo", "Cached screen data from ${data.sourceApp} (${current.size} total)")
            }
        }
    }

    override fun observeScreenData(): Flow<ScreenData> = screenDataFlow

    override suspend fun getRecent(maxCount: Int): List<ScreenData> {
        return _cache.value.take(maxCount)
    }

    override suspend fun clear() {
        _cache.value = emptyList()
        Logger.d("ScreenDataRepo", "Cleared screen data cache")
    }
}
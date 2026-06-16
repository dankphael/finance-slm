package com.habibi.financeslm.data.datasource

import com.habibi.financeslm.domain.model.LoraAdapter
import com.habibi.financeslm.util.Logger

/**
 * Stub LoraStorageDataSource for compilation.
 */
class LoraStorageDataSource {
    suspend fun saveAdapter(adapter: LoraAdapter) {
        Logger.d("LoraStorageDS", "saveAdapter(stub): ${adapter.id}")
    }

    suspend fun deleteAdapter(adapterId: String) {
        Logger.d("LoraStorageDS", "deleteAdapter(stub): $adapterId")
    }

    fun adapterExists(adapterId: String): Boolean = false
}
package com.habibi.financeslm.data.datasource

import com.habibi.financeslm.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Stub PreferencesDataSource for compilation.
 * Real implementation will use multiplatform-settings.
 */
class PreferencesDataSource {
    private val _prefs = mutableMapOf<String, String>()

    fun getString(key: String, default: String = ""): String = _prefs[key] ?: default

    suspend fun putString(key: String, value: String) {
        _prefs[key] = value
    }

    fun getBoolean(key: String, default: Boolean = false): Boolean {
        return _prefs[key]?.toBooleanStrictOrNull() ?: default
    }

    suspend fun putBoolean(key: String, value: Boolean) {
        _prefs[key] = value.toString()
    }

    fun getLong(key: String, default: Long = 0L): Long {
        return _prefs[key]?.toLongOrNull() ?: default
    }

    suspend fun putLong(key: String, value: Long) {
        _prefs[key] = value.toString()
    }

    suspend fun remove(key: String) {
        _prefs.remove(key)
    }

    suspend fun clear() {
        _prefs.clear()
    }
}
package com.habibi.financeslm.data.datasource

import com.habibi.financeslm.util.Logger
import com.russhwolf.settings.Settings

class PreferencesDataSource(
    private val settings: Settings? = null
) {
    private val prefs: Settings by lazy {
        settings ?: Settings()
    }

    init {
        Logger.d("PreferencesDataSource", "Initialized")
    }

    fun getString(key: String, default: String = ""): String {
        val value = prefs.getString(key, default)
        Logger.d("PreferencesDataSource", "getString($key) = $value")
        return value
    }

    fun putString(key: String, value: String) {
        prefs.putString(key, value)
        Logger.d("PreferencesDataSource", "putString($key, $value)")
    }

    fun getBoolean(key: String, default: Boolean = false): Boolean {
        val value = prefs.getBoolean(key, default)
        Logger.d("PreferencesDataSource", "getBoolean($key) = $value")
        return value
    }

    fun putBoolean(key: String, value: Boolean) {
        prefs.putBoolean(key, value)
        Logger.d("PreferencesDataSource", "putBoolean($key, $value)")
    }

    fun getLong(key: String, default: Long = 0L): Long {
        val value = prefs.getLong(key, default)
        Logger.d("PreferencesDataSource", "getLong($key) = $value")
        return value
    }

    fun putLong(key: String, value: Long) {
        prefs.putLong(key, value)
        Logger.d("PreferencesDataSource", "putLong($key, $value)")
    }

    fun remove(key: String) {
        prefs.remove(key)
        Logger.d("PreferencesDataSource", "remove($key)")
    }

    fun clear() {
        prefs.clear()
        Logger.d("PreferencesDataSource", "clear()")
    }
}
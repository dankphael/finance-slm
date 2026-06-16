package com.habibi.financeslm.data.repository

import com.habibi.financeslm.data.datasource.PreferencesDataSource
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.serializer
import com.habibi.financeslm.domain.model.AppPermission
import com.habibi.financeslm.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Real PreferencesRepository implementation backed by [PreferencesDataSource]
 * which uses multiplatform-settings for persistent key-value storage.
 */
class PreferencesRepositoryImpl(
    private val prefsDataSource: PreferencesDataSource
) : PreferencesRepository {

    companion object {
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        private const val KEY_SELECTED_MODEL_ID = "selected_model_id"
        private const val KEY_ACTIVE_LORA_ID = "active_lora_id"
        private const val KEY_MONITORED_PACKAGES = "monitored_packages"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_PERMISSION_PREFIX = "permission_"
    }

    // Reactive state for preferences that change at runtime
    private val _onboardingComplete = MutableStateFlow(prefsDataSource.getBoolean(KEY_ONBOARDING_COMPLETE, false))
    private val _selectedModelId = MutableStateFlow(prefsDataSource.getString(KEY_SELECTED_MODEL_ID).ifEmpty { null })
    private val _activeLoraId = MutableStateFlow(prefsDataSource.getString(KEY_ACTIVE_LORA_ID).ifEmpty { null })
    private val _firstLaunch = MutableStateFlow(prefsDataSource.getBoolean(KEY_FIRST_LAUNCH, true))

    override fun isOnboardingComplete(): Flow<Boolean> = _onboardingComplete.asStateFlow()

    override suspend fun setOnboardingComplete(value: Boolean) {
        prefsDataSource.putBoolean(KEY_ONBOARDING_COMPLETE, value)
        _onboardingComplete.value = value
    }

    override fun getSelectedModelId(): Flow<String?> = _selectedModelId.asStateFlow()

    override suspend fun setSelectedModelId(modelId: String?) {
        if (modelId != null) {
            prefsDataSource.putString(KEY_SELECTED_MODEL_ID, modelId)
        } else {
            prefsDataSource.remove(KEY_SELECTED_MODEL_ID)
        }
        _selectedModelId.value = modelId
    }

    override fun getActiveLoraId(): Flow<String?> = _activeLoraId.asStateFlow()

    override suspend fun setActiveLoraId(loraId: String?) {
        if (loraId != null) {
            prefsDataSource.putString(KEY_ACTIVE_LORA_ID, loraId)
        } else {
            prefsDataSource.remove(KEY_ACTIVE_LORA_ID)
        }
        _activeLoraId.value = loraId
    }

    override fun getPermissionState(permission: String): Flow<AppPermission> {
        val key = KEY_PERMISSION_PREFIX + permission
        val raw = prefsDataSource.getString(key)
        val state = try {
            AppPermission.valueOf(raw)
        } catch (_: Exception) {
            AppPermission.NOT_REQUESTED
        }
        return MutableStateFlow(state).asStateFlow()
    }

    override suspend fun setPermissionState(permission: String, state: AppPermission) {
        val key = KEY_PERMISSION_PREFIX + permission
        prefsDataSource.putString(key, state.name)
    }

    override fun isFirstLaunch(): Flow<Boolean> = _firstLaunch.asStateFlow()

    override suspend fun setFirstLaunchComplete() {
        prefsDataSource.putBoolean(KEY_FIRST_LAUNCH, false)
        _firstLaunch.value = false
    }

    fun getMonitoredPackages(): List<String> {
        val raw = prefsDataSource.getString(KEY_MONITORED_PACKAGES)
        if (raw.isEmpty()) return emptyList()
        return decodeList(raw)
    }

    /**
     * Set monitored packages as a JSON array string.
     */
    suspend fun setMonitoredPackages(packages: List<String>) {
        val raw = encodeList(packages)
        prefsDataSource.putString(KEY_MONITORED_PACKAGES, raw)
    }

    /**
     * Helper: encode a list of strings as JSON array.
     */
    private fun encodeList(values: List<String>): String {
        return try {
            kotlinx.serialization.json.Json { encodeDefaults = true }.encodeToString(
                ListSerializer(serializer<String>()),
                values
            )
        } catch (_: Exception) {
            "[]"
        }
    }

    /**
     * Helper: decode a JSON array to a list of strings.
     */
    private fun decodeList(raw: String): List<String> {
        return try {
            kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.decodeFromString(
                ListSerializer(serializer<String>()),
                raw
            )
        } catch (_: Exception) {
            emptyList()
        }
    }
}
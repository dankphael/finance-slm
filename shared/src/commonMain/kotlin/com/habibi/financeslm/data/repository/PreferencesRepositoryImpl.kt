package com.habibi.financeslm.data.repository

import com.habibi.financeslm.domain.model.AppPermission
import com.habibi.financeslm.domain.repository.PreferencesRepository
import com.habibi.financeslm.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Stub PreferencesRepository for compilation.
 * Real implementation will use multiplatform-settings.
 */
class PreferencesRepositoryImpl : PreferencesRepository {
    private val _onboardingComplete = MutableStateFlow(false)
    private val _selectedModelId = MutableStateFlow<String?>(null)
    private val _activeLoraId = MutableStateFlow<String?>(null)
    private val _permissions = MutableStateFlow<Map<String, AppPermission>>(emptyMap())
    private val _firstLaunch = MutableStateFlow(true)

    override fun isOnboardingComplete(): Flow<Boolean> = _onboardingComplete.asStateFlow()

    override suspend fun setOnboardingComplete(value: Boolean) {
        _onboardingComplete.value = value
    }

    override fun getSelectedModelId(): Flow<String?> = _selectedModelId.asStateFlow()

    override suspend fun setSelectedModelId(modelId: String?) {
        _selectedModelId.value = modelId
    }

    override fun getActiveLoraId(): Flow<String?> = _activeLoraId.asStateFlow()

    override suspend fun setActiveLoraId(loraId: String?) {
        _activeLoraId.value = loraId
    }

    override fun getPermissionState(permission: String): Flow<AppPermission> {
        return MutableStateFlow(_permissions.value[permission] ?: AppPermission.NOT_REQUESTED).asStateFlow()
    }

    override suspend fun setPermissionState(permission: String, state: AppPermission) {
        _permissions.value = _permissions.value + (permission to state)
    }

    override fun isFirstLaunch(): Flow<Boolean> = _firstLaunch.asStateFlow()

    override suspend fun setFirstLaunchComplete() {
        _firstLaunch.value = false
    }
}
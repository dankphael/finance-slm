package com.habibi.financeslm.domain.usecase

import com.habibi.financeslm.domain.model.AppPermission
import com.habibi.financeslm.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow

class CheckPermissionsUseCase(
    private val preferencesRepository: PreferencesRepository
) {
    fun getPermissionState(permission: String): Flow<AppPermission> {
        return preferencesRepository.getPermissionState(permission)
    }

    suspend fun setPermissionState(permission: String, state: AppPermission) {
        preferencesRepository.setPermissionState(permission, state)
    }

    fun isAllRequiredPermissionsGranted(): Boolean {
        // Implementation checks all required permissions
        return false // Stub
    }
}
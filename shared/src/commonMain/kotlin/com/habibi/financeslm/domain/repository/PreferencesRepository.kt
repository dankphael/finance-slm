package com.habibi.financeslm.domain.repository

import com.habibi.financeslm.domain.model.AppPermission
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    fun isOnboardingComplete(): Flow<Boolean>
    suspend fun setOnboardingComplete(value: Boolean)
    fun getSelectedModelId(): Flow<String?>
    suspend fun setSelectedModelId(modelId: String?)
    fun getActiveLoraId(): Flow<String?>
    suspend fun setActiveLoraId(loraId: String?)
    fun getPermissionState(permission: String): Flow<AppPermission>
    suspend fun setPermissionState(permission: String, state: AppPermission)
    fun isFirstLaunch(): Flow<Boolean>
    suspend fun setFirstLaunchComplete()
}
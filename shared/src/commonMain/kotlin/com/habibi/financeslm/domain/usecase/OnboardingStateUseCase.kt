package com.habibi.financeslm.domain.usecase

import com.habibi.financeslm.domain.repository.ModelRepository
import com.habibi.financeslm.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow

class OnboardingStateUseCase(
    private val preferencesRepository: PreferencesRepository,
    private val modelRepository: ModelRepository
) {
    fun isOnboardingComplete(): Flow<Boolean> {
        return preferencesRepository.isOnboardingComplete()
    }

    suspend fun completeOnboarding() {
        preferencesRepository.setOnboardingComplete(true)
    }

    fun hasDownloadedModel(): Flow<List<*>> {
        return modelRepository.getDownloadedModels()
    }
}
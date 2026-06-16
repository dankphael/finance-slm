package com.habibi.financeslm.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.habibi.financeslm.android.ui.navigation.AppNavGraph
import com.habibi.financeslm.android.ui.navigation.Screen
import com.habibi.financeslm.android.ui.theme.FinanceSlmTheme
import com.habibi.financeslm.domain.usecase.OnboardingStateUseCase
import org.koin.java.KoinJavaComponent.get
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check onboarding state synchronously (blocking is acceptable here
        // since the flow emits the current value immediately from StateFlow).
        val isOnboardingComplete = runBlocking {
            try {
                val useCase = get<OnboardingStateUseCase>(OnboardingStateUseCase::class.java)
                useCase.isOnboardingComplete().first()
            } catch (e: Exception) {
                false
            }
        }

        val startDestination = if (isOnboardingComplete) {
            Screen.Home.route
        } else {
            Screen.Onboarding.route
        }

        setContent {
            FinanceSlmTheme {
                AppNavGraph(startDestination = startDestination)
            }
        }
    }
}
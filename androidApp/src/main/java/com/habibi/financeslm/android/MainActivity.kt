package com.habibi.financeslm.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.habibi.financeslm.android.ui.navigation.AppNavGraph
import com.habibi.financeslm.android.ui.navigation.Screen
import com.habibi.financeslm.android.ui.theme.FinanceSlmTheme
import com.habibi.financeslm.domain.usecase.OnboardingStateUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.get

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Resolve the start destination off the main thread; the branded splash
        // stays up until it's ready (no more runBlocking / white-screen flash).
        var startDestination by mutableStateOf<String?>(null)
        splashScreen.setKeepOnScreenCondition { startDestination == null }

        lifecycleScope.launch {
            val isOnboardingComplete = try {
                val useCase = get<OnboardingStateUseCase>(OnboardingStateUseCase::class.java)
                useCase.isOnboardingComplete().first()
            } catch (e: Exception) {
                false
            }
            startDestination = if (isOnboardingComplete) Screen.Home.route else Screen.Onboarding.route
        }

        setContent {
            FinanceSlmTheme {
                startDestination?.let { destination ->
                    AppNavGraph(startDestination = destination)
                }
            }
        }
    }
}

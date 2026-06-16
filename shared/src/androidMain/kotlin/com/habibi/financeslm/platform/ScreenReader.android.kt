package com.habibi.financeslm.platform

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.accessibility.AccessibilityManager
import android.os.Handler
import android.widget.Toast
import com.habibi.financeslm.domain.model.ScreenData
import com.habibi.financeslm.service.ScreenReaderBridge
import com.habibi.financeslm.util.Logger
import kotlinx.coroutines.flow.Flow

/**
 * Android ScreenReader — connects to FinanceScreenReaderService via ScreenReaderBridge.
 *
 * When the AccessibilityService is enabled by the user, ScreenReaderBridge.observeData()
 * emits real ScreenData from the service. When disabled, the flow simply never emits
 * (which is handled gracefully by consumers).
 */
class AndroidScreenReader : ScreenReader {
    private var running = false

    override fun observeScreenData(): Flow<ScreenData> = ScreenReaderBridge.observeData()

    override suspend fun start() {
        running = true
        Logger.d("ScreenReader", "ScreenReader started (Android)")

        // Check if AccessibilityService is enabled and warn if not
        val androidContext = PlatformContext.getInstance().androidContext
        val accessibilityManager =
            androidContext.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
                ?: return

        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_GENERIC
        )

        val ourServiceEnabled = enabledServices.any { service ->
            service.resolveInfo?.serviceInfo?.packageName == androidContext.packageName
        }

        if (!ourServiceEnabled) {
            Logger.w("ScreenReader", "AccessibilityService is NOT enabled — no screen data will be captured")
            // Show a toast so the user knows
            android.os.Handler(androidContext.mainLooper).post {
                Toast.makeText(
                    androidContext,
                    "Enable Accessibility Service in Settings for screen reading",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            Logger.d("ScreenReader", "AccessibilityService is enabled")
        }
    }

    override suspend fun stop() {
        running = false
        Logger.d("ScreenReader", "ScreenReader stopped (Android)")
    }

    override fun isRunning(): Boolean = running

    /**
     * Check whether the AccessibilityService is currently enabled in system settings.
     */
    fun isServiceEnabled(): Boolean {
        val androidContext = PlatformContext.getInstance().androidContext
        val accessibilityManager =
            androidContext.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
                ?: return false

        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_GENERIC
        )

        return enabledServices.any { service ->
            service.resolveInfo?.serviceInfo?.packageName == androidContext.packageName
        }
    }
}

actual fun createScreenReader(): ScreenReader = AndroidScreenReader()
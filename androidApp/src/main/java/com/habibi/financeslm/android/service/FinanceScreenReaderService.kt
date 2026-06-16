package com.habibi.financeslm.android.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * FinanceScreenReaderService — Android AccessibilityService that listens for
 * screen content changes from configured finance apps (PayNow, PayLah, Grab, Moomoo, Tiger Brokers).
 *
 * Stub for compilation. Full implementation in Phase 6.
 */
class FinanceScreenReaderService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Stub — will extract text from AccessibilityNodeInfo tree and filter by packageName
    }

    override fun onInterrupt() {
        // Service was interrupted
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Stub — will set up package name filtering
    }
}
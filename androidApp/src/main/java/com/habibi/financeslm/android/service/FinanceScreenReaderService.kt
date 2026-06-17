package com.habibi.financeslm.android.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat
import com.habibi.financeslm.android.MainActivity
import com.habibi.financeslm.domain.model.DataPoint
import com.habibi.financeslm.domain.model.DataPointType
import com.habibi.financeslm.domain.model.ScreenData
import com.habibi.financeslm.service.ScreenReaderBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.regex.Pattern

/**
 * FinanceScreenReaderService — Android AccessibilityService that listens for
 * screen content changes from configured Singapore finance apps.
 *
 * Extracts text from AccessibilityNodeInfo trees, parses financial data points
 * (amounts, dates, balances, merchants), and sends parsed ScreenData to the
 * main app via ScreenReaderBridge.
 */
class FinanceScreenReaderService : AccessibilityService() {

    companion object {
        private const val TAG = "FinanceScreenReader"

        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "finance_slm_screen_reader"

        /** Allowed finance apps — only capture from these. */
        private val ALLOWED_PACKAGES = setOf(
            "com.dbs.sg.dbsmbanking",   // DBS/PayNow
            "com.dbs.paylah",            // PayLah
            "com.ocbc.mobile",           // OCBC
            "com.uob.mighty.app",        // UOB
            "com.grabtaxi.passenger",    // Grab
            "com.moomoo.sg",             // Moomoo
            "com.tigerbrokers.stock"     // Tiger Brokers
        )

        /** Human-readable app name lookup. */
        private val APP_NAMES = mapOf(
            "com.dbs.sg.dbsmbanking" to "DBS/PayNow",
            "com.dbs.paylah" to "PayLah",
            "com.ocbc.mobile" to "OCBC",
            "com.uob.mighty.app" to "UOB",
            "com.grabtaxi.passenger" to "Grab",
            "com.moomoo.sg" to "Moomoo",
            "com.tigerbrokers.stock" to "Tiger Brokers"
        )

        /** Our own app package — MUST never capture from this. */
        private const val OWN_PACKAGE = "com.habibi.financeslm"

        // ---- Regex patterns for financial data extraction ----

        /** Matches dollar amounts: $1,234.56 or SGD 1,234.56 or S$1,234.56 */
        private val AMOUNT_PATTERN = Pattern.compile(
            """(?:SGD\s*|S?\$)\s*\d{1,3}(?:,\d{3})*(?:\.\d{2})?"""
        )

        /** Matches dates: "15 January 2024" or "15/01/2024" or "15/01/24" */
        private val DATE_PATTERN = Pattern.compile(
            """\b\d{1,2}\s+(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\s+\d{4}\b|\b\d{1,2}/\d{1,2}/\d{2,4}\b"""
        )

        /** Keywords that indicate a balance line. */
        private val BALANCE_KEYWORDS = setOf(
            "balance", "available", "total", "current balance",
            "available balance", "account balance", "savings",
            "cheque account", "current account"
        )

        /** Keywords that indicate a transaction description / merchant name. */
        private val DESCRIPTION_KEYWORDS = setOf(
            "to", "from", "paid", "received", "transfer", "payment",
            "purchase", "refund", "transaction", "merchant"
        )
    }

    /** Coroutine scope for launching non-blocking parse+send operations. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onServiceConnected() {
        super.onServiceConnected()

        // Configure service info
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED

            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC

            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS

            notificationTimeout = 500

            // Package-level filter — only receive events from these apps
            packageNames = ALLOWED_PACKAGES.toTypedArray()
        }

        createNotificationChannel()
        startForegroundService()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return

        // Double-check: only process events from allowed packages
        if (packageName !in ALLOWED_PACKAGES) return

        // Privacy: skip notification events to avoid capturing OTPs/sensitive notifications
        if (event.eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) return

        // Extract the root node
        val rootNode = event.source ?: return

        // Extract text on the default dispatcher to avoid blocking the callback
        scope.launch {
            try {
                val extractedTexts = mutableListOf<String>()
                collectText(rootNode, extractedTexts)

                if (extractedTexts.isEmpty()) return@launch

                val fullText = extractedTexts.joinToString("\n")

                // Parse data points from extracted text
                val dataPoints = parseDataPoints(fullText)

                val screenData = ScreenData(
                    id = UUID.randomUUID().toString(),
                    sourcePackage = packageName,
                    sourceApp = APP_NAMES[packageName] ?: packageName,
                    textContent = fullText,
                    timestamp = System.currentTimeMillis(),
                    dataPoints = dataPoints
                )

                // Send to main app via singleton bridge
                ScreenReaderBridge.sendData(screenData)
            } catch (e: Exception) {
                // Silently swallow — never crash the AccessibilityService
                android.util.Log.e(TAG, "Error processing accessibility event", e)
            } finally {
                rootNode.recycle()
            }
        }
    }

    override fun onInterrupt() {
        // Service was interrupted — Android may restart it
        android.util.Log.d(TAG, "AccessibilityService interrupted")
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
        android.util.Log.d(TAG, "AccessibilityService destroyed")
    }

    // ---------------------------------------------------------------
    // Text extraction from AccessibilityNodeInfo tree
    // ---------------------------------------------------------------

    /**
     * Iteratively traverse the AccessibilityNodeInfo tree using a stack (ArrayDeque)
     * to avoid StackOverflowError on deeply nested UIs (e.g., nested RecyclerViews).
     * Skips password fields and empty nodes.
     */
    private fun collectText(node: AccessibilityNodeInfo, results: MutableList<String>) {
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.addLast(node)

        while (stack.isNotEmpty()) {
            val current = stack.removeLastOrNull() ?: continue

            // CRITICAL: Skip password fields
            if (current.isPassword) {
                current.recycle()
                continue
            }

            // Collect text from this node
            val text = current.text?.toString()?.trim()
            if (!text.isNullOrBlank()) {
                results.add(text)
            }

            // Collect content description (sometimes apps put data here)
            val contentDesc = current.contentDescription?.toString()?.trim()
            if (!contentDesc.isNullOrBlank()) {
                results.add(contentDesc)
            }

            // Push child nodes onto the stack (reverse order so children
            // are processed in original order when popped)
            for (i in (current.childCount - 1) downTo 0) {
                val child = current.getChild(i) ?: continue
                stack.addLast(child)
            }

            current.recycle()
        }
    }

    // ---------------------------------------------------------------
    // Financial data parsing
    // ---------------------------------------------------------------

    /**
     * Parse financial data points from extracted screen text.
     */
    private fun parseDataPoints(text: String): List<DataPoint> {
        val dataPoints = mutableListOf<DataPoint>()
        val lines = text.lines()

        // 1. Extract amounts
        val amountMatcher = AMOUNT_PATTERN.matcher(text)
        while (amountMatcher.find()) {
            val value = amountMatcher.group()
            dataPoints.add(
                DataPoint(
                    type = DataPointType.AMOUNT,
                    label = "Transaction Amount",
                    value = value,
                    currency = determineCurrency(value)
                )
            )
        }

        // 2. Extract dates
        val dateMatcher = DATE_PATTERN.matcher(text)
        while (dateMatcher.find()) {
            dataPoints.add(
                DataPoint(
                    type = DataPointType.DATE,
                    label = "Date",
                    value = dateMatcher.group()
                )
            )
        }

        // 3. Extract balances — look for lines containing balance keywords with amounts
        for (line in lines) {
            val lowerLine = line.lowercase()
            if (BALANCE_KEYWORDS.any { lowerLine.contains(it) }) {
                // Try to extract the amount on this or next line
                val balanceMatcher = AMOUNT_PATTERN.matcher(line)
                if (balanceMatcher.find()) {
                    dataPoints.add(
                        DataPoint(
                            type = DataPointType.BALANCE,
                            label = line.trim(),
                            value = balanceMatcher.group(),
                            currency = determineCurrency(balanceMatcher.group())
                        )
                    )
                }
            }
        }

        // 4. Extract merchant/payee names — text near description keywords
        for (line in lines) {
            val lowerLine = line.lowercase()
            if (DESCRIPTION_KEYWORDS.any { lowerLine.contains(it) }) {
                // Clean the line and add as a description data point
                val clean = line.trim().replace(AMOUNT_PATTERN.toRegex(), "").trim()
                if (clean.isNotBlank()) {
                    dataPoints.add(
                        DataPoint(
                            type = DataPointType.DESCRIPTION,
                            label = "Transaction",
                            value = clean.take(100) // Cap length
                        )
                    )
                }
            }
        }

        return dataPoints
    }

    private fun determineCurrency(value: String): String? {
        return when {
            value.startsWith("SGD") || value.startsWith("S$") || value.startsWith("$") -> "SGD"
            else -> null
        }
    }

    // ---------------------------------------------------------------
    // Notification / Foreground Service
    // ---------------------------------------------------------------

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Reading",
                NotificationManager.IMPORTANCE_LOW  // Low importance — non-intrusive
            ).apply {
                description = "Finance SLM screen reading service notification"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun startForegroundService() {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Finance SLM Active")
            .setContentText("Monitoring finance apps for insights")
            .setSmallIcon(android.R.drawable.ic_menu_manage) // Fallback — replace with custom icon
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }
}

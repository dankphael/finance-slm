package com.habibi.financeslm.android.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Human-readable file size, e.g. 1_500_000_000 -> "1.5 GB". Uses decimal (SI)
 * units to match how model sizes are advertised. Replaces the size-formatting
 * math that was previously duplicated across the model screens.
 */
fun formatFileSize(bytes: Long): String = when {
    bytes >= 1_000_000_000L -> String.format(Locale.US, "%.1f GB", bytes / 1_000_000_000.0)
    bytes >= 1_000_000L -> "${bytes / 1_000_000} MB"
    bytes >= 1_000L -> "${bytes / 1_000} KB"
    else -> "$bytes B"
}

private val timestampFormat = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())

/** Formats an epoch-millis timestamp as e.g. "18 Jun 14:30". */
fun formatTimestamp(millis: Long): String = timestampFormat.format(Date(millis))

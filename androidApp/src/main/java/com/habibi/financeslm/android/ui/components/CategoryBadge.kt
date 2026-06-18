package com.habibi.financeslm.android.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.habibi.financeslm.domain.model.InsightCategory

/**
 * Pill-shaped category label for finance insights. Colour is mapped from the
 * active [MaterialTheme] colour scheme (so it adapts to dark mode) rather than
 * being hardcoded at the call site.
 */
@Composable
fun CategoryBadge(category: InsightCategory, modifier: Modifier = Modifier) {
    val (label, color) = category.labelAndColor()
    Surface(
        modifier = modifier.semantics { contentDescription = "Category: $label" },
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
private fun InsightCategory.labelAndColor(): Pair<String, Color> = when (this) {
    InsightCategory.SPENDING -> "Spending" to MaterialTheme.colorScheme.tertiary
    InsightCategory.SAVINGS -> "Savings" to MaterialTheme.colorScheme.primary
    InsightCategory.INVESTMENT -> "Investment" to MaterialTheme.colorScheme.secondary
    InsightCategory.BUDGET -> "Budget" to MaterialTheme.colorScheme.error
    InsightCategory.GENERAL -> "General" to MaterialTheme.colorScheme.outline
}

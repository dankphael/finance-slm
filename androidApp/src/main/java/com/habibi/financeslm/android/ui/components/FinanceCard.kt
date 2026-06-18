package com.habibi.financeslm.android.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.habibi.financeslm.android.ui.theme.Spacing

/**
 * Standard app card: full width, consistent inner padding ([Spacing.lg]) and the
 * themed [androidx.compose.material3.Shapes.medium] corner. Use instead of a raw
 * [Card] + [Column] + padding so every card looks the same.
 */
@Composable
fun FinanceCard(
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.cardColors(),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = colors
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            content = content
        )
    }
}

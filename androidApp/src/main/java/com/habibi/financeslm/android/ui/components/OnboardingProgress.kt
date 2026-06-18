package com.habibi.financeslm.android.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Step indicator for the onboarding flow: the active step is a stretched pill,
 * the rest are dots. [step] is 1-based.
 */
@Composable
fun OnboardingProgress(
    step: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.semantics { contentDescription = "Step $step of $total" }
    ) {
        for (i in 1..total) {
            val active = i == step
            val color by animateColorAsState(
                targetValue = if (active || i < step) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                label = "dot-color"
            )
            val width by animateDpAsState(
                targetValue = if (active) 24.dp else 8.dp,
                label = "dot-width"
            )
            Spacer(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(color)
            )
            if (i < total) Spacer(Modifier.width(6.dp))
        }
    }
}

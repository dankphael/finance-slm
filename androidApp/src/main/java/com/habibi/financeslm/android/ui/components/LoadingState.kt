package com.habibi.financeslm.android.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.habibi.financeslm.android.ui.theme.Spacing

/**
 * A single shimmering placeholder bar. Use [SkeletonCard] for a card-shaped
 * placeholder or compose these for custom layouts.
 */
@Composable
fun ShimmerBar(
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 16.dp,
    fraction: Float = 1f
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer-alpha"
    )
    Spacer(
        modifier = modifier
            .fillMaxWidth(fraction)
            .height(height)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
    )
}

/** A card-shaped loading placeholder approximating a model/insight row. */
@Composable
fun SkeletonCard(modifier: Modifier = Modifier) {
    FinanceCard(modifier = modifier) {
        ShimmerBar(fraction = 0.6f, height = 20.dp)
        Spacer(Modifier.height(Spacing.sm))
        ShimmerBar(fraction = 0.9f)
        Spacer(Modifier.height(Spacing.xs))
        ShimmerBar(fraction = 0.4f)
        Spacer(Modifier.height(Spacing.md))
        ShimmerBar(height = 40.dp)
    }
}

/** A vertical stack of [SkeletonCard]s for list placeholders. */
@Composable
fun SkeletonList(count: Int = 3, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(Spacing.lg)) {
        repeat(count) {
            SkeletonCard()
            Spacer(Modifier.height(Spacing.sm))
        }
    }
}

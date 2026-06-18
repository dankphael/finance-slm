package com.habibi.financeslm.android.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Slightly softer, rounded shape scale for a friendly, modern finance look.
 * Cards use [Shapes.medium] (16dp) by default via [com.habibi.financeslm.android.ui.components.FinanceCard].
 */
val FinanceShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

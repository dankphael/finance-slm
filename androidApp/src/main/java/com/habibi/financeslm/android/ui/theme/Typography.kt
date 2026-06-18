package com.habibi.financeslm.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.habibi.financeslm.android.R

/**
 * Inter — a clean, highly legible humanist sans that reads well for numbers and
 * dense financial text. Bundled as a single variable font (res/font/inter.ttf,
 * SIL OFL — see assets/inter_font_license.txt); per-weight instances are derived
 * via [FontVariation] (supported on minSdk 26+).
 */
@OptIn(ExperimentalTextApi::class)
private fun interFont(weight: FontWeight) = Font(
    R.font.inter,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

val InterFontFamily = FontFamily(
    interFont(FontWeight.Normal),
    interFont(FontWeight.Medium),
    interFont(FontWeight.SemiBold),
    interFont(FontWeight.Bold)
)

// Start from Material 3 defaults so every text role is covered, then apply the
// Inter family and tune the most-used display/headline/title styles.
private val base = Typography()

val FinanceSlmTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = InterFontFamily),
    displayMedium = base.displayMedium.copy(fontFamily = InterFontFamily),
    displaySmall = base.displaySmall.copy(fontFamily = InterFontFamily),
    headlineLarge = base.headlineLarge.copy(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineMedium = base.headlineMedium.copy(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    headlineSmall = base.headlineSmall.copy(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    titleLarge = base.titleLarge.copy(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    titleMedium = base.titleMedium.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Medium),
    bodyLarge = base.bodyLarge.copy(
        fontFamily = InterFontFamily,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = base.bodyMedium.copy(
        fontFamily = InterFontFamily,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = base.bodySmall.copy(fontFamily = InterFontFamily),
    labelLarge = base.labelLarge.copy(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = base.labelMedium.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Medium),
    labelSmall = base.labelSmall.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Medium)
)

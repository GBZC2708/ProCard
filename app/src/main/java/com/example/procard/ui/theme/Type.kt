package com.example.procard.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.times

private val BaseTypography = Typography()

private fun TextStyle.scale(factor: Float): TextStyle {
    fun scaleUnit(unit: TextUnit): TextUnit = if (unit.isUnspecified) unit else unit * factor
    return copy(
        fontSize = scaleUnit(fontSize),
        lineHeight = scaleUnit(lineHeight),
        letterSpacing = scaleUnit(letterSpacing)
    )
}

val Typography: Typography = BaseTypography.copy(
    displayLarge = BaseTypography.displayLarge.scale(0.9f),
    displayMedium = BaseTypography.displayMedium.scale(0.9f),
    displaySmall = BaseTypography.displaySmall.scale(0.9f),
    headlineLarge = BaseTypography.headlineLarge.scale(0.92f),
    headlineMedium = BaseTypography.headlineMedium.scale(0.92f),
    headlineSmall = BaseTypography.headlineSmall.scale(0.92f),
    titleLarge = BaseTypography.titleLarge.scale(0.9f),
    titleMedium = BaseTypography.titleMedium.scale(0.9f),
    titleSmall = BaseTypography.titleSmall.scale(0.9f),
    bodyLarge = BaseTypography.bodyLarge.scale(0.9f),
    bodyMedium = BaseTypography.bodyMedium.scale(0.9f),
    bodySmall = BaseTypography.bodySmall.scale(0.9f),
    labelLarge = BaseTypography.labelLarge.scale(0.9f),
    labelMedium = BaseTypography.labelMedium.scale(0.9f),
    labelSmall = BaseTypography.labelSmall.scale(0.9f)
)

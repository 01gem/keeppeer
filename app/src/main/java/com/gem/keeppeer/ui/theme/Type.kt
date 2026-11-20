package com.gem.keeppeer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.gem.keeppeer.R

// Onest ships only as a variable font (wght 100-900). Each weight is an instance of the
// same bundled file. API 26+ applies the variation; API 24-25 falls back to synthetic bold.
private fun onest(weight: Int) = Font(
    R.font.onest,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Onest = FontFamily(onest(400), onest(500), onest(600), onest(700))

/** Only for revealed password characters. */
val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
)

private fun style(size: Int, weight: FontWeight, line: Int, tracking: Double = 0.0) = TextStyle(
    fontFamily = Onest,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.em,
)

val Typography = Typography(
    displayLarge = style(36, FontWeight.Bold, 44, -0.02),
    displayMedium = style(32, FontWeight.Bold, 40, -0.02),
    displaySmall = style(30, FontWeight.Bold, 38, -0.02),
    headlineLarge = style(28, FontWeight.Bold, 36, -0.02),
    headlineMedium = style(28, FontWeight.Bold, 36, -0.02), // screen title
    headlineSmall = style(26, FontWeight.Bold, 32, -0.01), // entry title
    titleLarge = style(20, FontWeight.SemiBold, 28),
    titleMedium = style(16, FontWeight.SemiBold, 24),
    titleSmall = style(14, FontWeight.SemiBold, 20),
    bodyLarge = style(16, FontWeight.Normal, 24),
    bodyMedium = style(14, FontWeight.Normal, 20),
    bodySmall = style(13, FontWeight.Normal, 18),
    labelLarge = style(16, FontWeight.SemiBold, 22), // buttons
    labelMedium = style(14, FontWeight.Medium, 20),
    labelSmall = style(13, FontWeight.Medium, 18),
)

/** Revealed password character tiles. Kept out of [Typography] on purpose. */
val PasswordCharStyle = TextStyle(
    fontFamily = JetBrainsMono,
    fontWeight = FontWeight.Bold,
    fontSize = 20.sp,
    lineHeight = 24.sp,
)

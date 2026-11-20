package com.gem.keeppeer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.compose.ui.unit.dp

val LocalThemeMode = staticCompositionLocalOf { com.gem.keeppeer.ui.settings.ThemeMode.SYSTEM }

/** Brand colors that Material's ColorScheme has no role for. */
@Immutable
data class KeeppeerColors(
    val surfaceHigh: Color,
    val muted: Color,
    /** Fill behind white text: filled buttons, selected segment. */
    val buttonFill: Color,
    val onButtonFill: Color,
    /** Silver brand color: outline button border, shield right half, monogram variant. */
    val steel: Color,
    val letter: Color,
    val letterBg: Color,
    val digit: Color,
    val digitBg: Color,
    val symbol: Color,
    val symbolBg: Color,
    val success: Color,
    val warningBg: Color,
    val warningText: Color,
    val warningIcon: Color,
    /** Text-button color: dark 6.6:1 on dialogs (surfaceHigh), light 4.7:1. */
    val link: Color,
    val isDark: Boolean,
)

private val DarkExtended = KeeppeerColors(
    surfaceHigh = DarkSurfaceHigh, muted = DarkMuted,
    buttonFill = DarkButtonFill, onButtonFill = OnPrimary, steel = DarkSteel,
    letter = DarkOnSurface, letterBg = DarkSurfaceHigh,
    digit = DarkDigit, digitBg = DarkDigitBg, symbol = DarkSymbol, symbolBg = DarkSymbolBg,
    success = DarkSuccess, warningBg = DarkWarningBg, warningText = DarkWarningText,
    warningIcon = DarkWarningIcon, link = Color(0xFF8AB4FF), isDark = true,
)

private val LightExtended = KeeppeerColors(
    surfaceHigh = LightSurfaceHigh, muted = LightMuted,
    buttonFill = LightPrimary, onButtonFill = OnPrimary, steel = LightSteel,
    letter = LightOnSurface, letterBg = LightSurfaceHigh,
    digit = LightDigit, digitBg = LightDigitBg, symbol = LightSymbol, symbolBg = LightSymbolBg,
    success = LightSuccess, warningBg = LightWarningBg, warningText = LightWarningText,
    warningIcon = LightWarningIcon, link = LightPrimary, isDark = false,
)

val LocalKeeppeerColors = staticCompositionLocalOf { DarkExtended }

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = OnPrimary,
    primaryContainer = DarkButtonFill,
    onPrimaryContainer = OnPrimary,
    secondary = DarkSteel,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSurfaceHigh,
    onSecondaryContainer = DarkOnSurface,
    tertiary = DarkSymbol,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceHigh,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceTint = Color.Transparent,
    surfaceDim = DarkBackground,
    surfaceBright = DarkSurfaceHigh,
    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkSurfaceHigh,
    surfaceContainerHighest = DarkSurfaceHigh,
    inverseSurface = DarkOnSurface,
    inverseOnSurface = DarkBackground,
    inversePrimary = LightPrimary,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = DarkError,
    onError = DarkBackground,
    errorContainer = Color(0xFF3A1A1F),
    onErrorContainer = Color(0xFFFFDAD5),
    scrim = Color.Black,
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = OnPrimary,
    primaryContainer = LightPrimary,
    onPrimaryContainer = OnPrimary,
    secondary = LightSteel,
    onSecondary = OnPrimary,
    secondaryContainer = LightSurfaceHigh,
    onSecondaryContainer = LightOnSurface,
    tertiary = LightSymbol,
    onTertiary = OnPrimary,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceHigh,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceTint = Color.Transparent,
    surfaceDim = LightSurfaceHigh,
    surfaceBright = LightSurface,
    surfaceContainerLowest = LightSurface,
    surfaceContainerLow = LightSurface,
    surfaceContainer = LightSurface,
    surfaceContainerHigh = LightSurfaceHigh,
    surfaceContainerHighest = LightSurfaceHigh,
    inverseSurface = LightOnSurface,
    inverseOnSurface = LightBackground,
    inversePrimary = DarkPrimary,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = LightError,
    onError = OnPrimary,
    errorContainer = Color(0xFFFDE7E7),
    onErrorContainer = Color(0xFF7A1010),
    scrim = Color.Black,
)

private val KeeppeerShapes = Shapes(
    extraSmall = RoundedCornerShape(16.dp), // text fields
    small = RoundedCornerShape(12.dp), // chips
    medium = RoundedCornerShape(16.dp), // fields, buttons
    large = RoundedCornerShape(24.dp), // cards
    extraLarge = RoundedCornerShape(28.dp), // dialogs, sheets
)

@Composable
fun KeeppeerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // No dynamic color: the vault-steel palette always applies.
    CompositionLocalProvider(LocalKeeppeerColors provides if (darkTheme) DarkExtended else LightExtended) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = Typography,
            shapes = KeeppeerShapes,
        ) {
            MatchSystemBars()
            content()
        }
    }
}

/**
 * Status/navigation bar icon colors follow Keeppeer's theme, not the phone's dark mode.
 * Call once inside any full-screen dialog too; it resolves that dialog's own window.
 */
@Composable
fun MatchSystemBars() {
    val view = LocalView.current
    val dark = LocalKeeppeerColors.current.isDark
    if (view.isInEditMode) return
    SideEffect {
        val window = (view.parent as? DialogWindowProvider)?.window
            ?: generateSequence(view.context) { (it as? android.content.ContextWrapper)?.baseContext }
                .filterIsInstance<android.app.Activity>().firstOrNull()?.window
            ?: return@SideEffect
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
}

object KeeppeerTheme {
    val colors: KeeppeerColors
        @Composable @ReadOnlyComposable get() = LocalKeeppeerColors.current
}

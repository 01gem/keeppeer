package com.gem.keeppeer.ui.theme

import androidx.compose.ui.graphics.Color

// "Vault steel" tokens, taken from the app icon: split blue/silver shield on navy.
// Contrast notes (WCAG, checked):
// - Filled buttons and the selected segment use ButtonFill + white (dark 4.98:1, light 5.57:1).
//   The dark accent #2F7BFF is only for non-text marks (switches, rings, icons, focus): white on it is 3.89:1.
// - Dark accent #2F7BFF as text passes on Ink (4.79:1) but not on Hull (4.23:1): no accent text on cards.
// - Dark muted #7D8DAD fails on SurfaceHigh (4.14:1): secondary text on tiles uses OnSurfaceVariant.

// Dark (default look)
internal val DarkBackground = Color(0xFF071229) // Ink
internal val DarkSurface = Color(0xFF0E1E3F) // Hull
internal val DarkSurfaceHigh = Color(0xFF162B57)
// Outline = control borders (fields, unselected chips, outline buttons, switch off): >= 3:1.
// OutlineVariant = decorative only (card/group borders, dividers, rings).
internal val DarkOutline = Color(0xFF5C77AD) // 3.68:1 on surface, 4.17:1 on background
internal val DarkOutlineVariant = Color(0xFF24407A)
internal val DarkOnSurface = Color(0xFFE3E8F2)
internal val DarkOnSurfaceVariant = Color(0xFF9DADCB)
internal val DarkMuted = Color(0xFF7D8DAD)
internal val DarkPrimary = Color(0xFF2F7BFF) // Signal blue accent
internal val DarkButtonFill = Color(0xFF1F66F0)
internal val DarkSteel = Color(0xFFC9D2E2)
internal val DarkDigit = Color(0xFF6FA6FF)
internal val DarkDigitBg = Color(0xFF13306A)
internal val DarkSymbol = Color(0xFFFFC266)
internal val DarkSymbolBg = Color(0xFF2A2A3E)
internal val DarkSuccess = Color(0xFF8FD6A8)
internal val DarkWarningBg = Color(0xFF2A2418)
internal val DarkWarningText = Color(0xFFFFD9A0)
internal val DarkWarningIcon = Color(0xFFFFC266)
internal val DarkError = Color(0xFFFF7A6B)

// Light
internal val LightBackground = Color(0xFFF3F6FB)
internal val LightSurface = Color(0xFFFFFFFF)
internal val LightSurfaceHigh = Color(0xFFE6ECF6)
internal val LightOutline = Color(0xFF7A8AA8) // 3.48:1 on white, 3.22:1 on background
internal val LightOutlineVariant = Color(0xFFC5D0E3)
internal val LightOnSurface = Color(0xFF0B1A38)
internal val LightOnSurfaceVariant = Color(0xFF4A5A7A)
internal val LightMuted = Color(0xFF586885) // 4.74:1 on SurfaceHigh
internal val LightPrimary = Color(0xFF1F5FE0) // white on it 5.57:1, so it is also the button fill
internal val LightSteel = Color(0xFF5C6B85)
internal val LightDigit = Color(0xFF1A4FBF) // 6.03:1 on its tile
internal val LightDigitBg = Color(0xFFE2EBFF)
internal val LightSymbol = Color(0xFF8A4B00) // 6.06:1 on its tile
internal val LightSymbolBg = Color(0xFFFFF0D9)
internal val LightSuccess = Color(0xFF1E7A45)
internal val LightWarningBg = Color(0xFFFFF4E0)
internal val LightWarningText = Color(0xFF6B4300)
internal val LightWarningIcon = Color(0xFFB26A00)
internal val LightError = Color(0xFFC62828)

internal val OnPrimary = Color(0xFFFFFFFF)

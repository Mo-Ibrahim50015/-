package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Serene Balance — Light Palette (exact tokens from DESIGN.md & screens)
val SerenePrimary = Color(0xFF091426)
val SereneOnPrimary = Color(0xFFFFFFFF)
val SerenePrimaryContainer = Color(0xFF1E293B)
val SereneOnPrimaryContainer = Color(0xFF8590A6)
val SerenePrimaryFixed = Color(0xFFD8E3FB)
val SereneOnPrimaryFixed = Color(0xFF111C2D)

val SereneSecondary = Color(0xFF006C4A)
val SereneOnSecondary = Color(0xFFFFFFFF)
val SereneSecondaryContainer = Color(0xFF82F5C1)
val SereneOnSecondaryContainer = Color(0xFF00714E)
val SereneSecondaryFixed = Color(0xFF85F8C4)
val SereneOnSecondaryFixed = Color(0xFF002114)

val SereneTertiary = Color(0xFF320008)
val SereneOnTertiary = Color(0xFFFFFFFF)
val SereneTertiaryContainer = Color(0xFF590015)
val SereneOnTertiaryContainer = Color(0xFFFF4E67)
val SereneTertiaryFixed = Color(0xFFFFDADA)

val SereneError = Color(0xFFBA1A1A)
val SereneOnError = Color(0xFFFFFFFF)
val SereneErrorContainer = Color(0xFFFFDAD6)
val SereneOnErrorContainer = Color(0xFF93000A)

val SereneSurface = Color(0xFFF7F9FB)
val SereneSurfaceLowest = Color(0xFFFFFFFF)
val SereneSurfaceLow = Color(0xFFF2F4F6)
val SereneSurfaceContainer = Color(0xFFECEEF0)
val SereneSurfaceHigh = Color(0xFFE6E8EA)
val SereneSurfaceHighest = Color(0xFFE0E3E5)
val SereneOnSurface = Color(0xFF191C1E)
val SereneOnSurfaceVariant = Color(0xFF45474C)
val SereneOutline = Color(0xFF75777D)
val SereneOutlineVariant = Color(0xFFE2E8F0)
val SereneSurfaceTint = Color(0xFF545F73)

// Serene Balance — Derived Dark Palette (calm deep slate with dignified emerald & soft rose)
val DarkSurface = Color(0xFF0B111B)
val DarkSurfaceLowest = Color(0xFF111B2B)
val DarkSurfaceLow = Color(0xFF172438)
val DarkSurfaceContainer = Color(0xFF1E2D45)
val DarkSurfaceHigh = Color(0xFF263854)
val DarkSurfaceHighest = Color(0xFF304566)
val DarkOnSurface = Color(0xFFF1F5F9)
val DarkOnSurfaceVariant = Color(0xFF94A3B8)
val DarkOutline = Color(0xFF64748B)
val DarkOutlineVariant = Color(0xFF23324A)

val DarkPrimary = Color(0xFFD8E3FB)
val DarkOnPrimary = Color(0xFF091426)
val DarkPrimaryContainer = Color(0xFF1E293B)
val DarkOnPrimaryContainer = Color(0xFFCBD5E1)

val DarkSecondary = Color(0xFF34D399)
val DarkOnSecondary = Color(0xFF002114)
val DarkSecondaryContainer = Color(0xFF064E3B)
val DarkOnSecondaryContainer = Color(0xFF85F8C4)

val DarkError = Color(0xFFF87171)
val DarkOnError = Color(0xFF450A0A)
val DarkErrorContainer = Color(0xFF4C1D24)
val DarkOnErrorContainer = Color(0xFFFFDAD6)

val DarkRoseAccent = Color(0xFFFB7185)

@Immutable
data class SereneExtendedColors(
    val cardBackground: Color,
    val cardSubtleBackground: Color,
    val cardContainer: Color,
    val cardBorder: Color,
    val heroCardBackground: Color,
    val heroCardAccentCircle: Color,
    val heroCardOnBackground: Color,
    val heroCardSubtext: Color,
    val incomeColor: Color,
    val incomeContainer: Color,
    val expenseColor: Color,
    val expenseContainer: Color,
    val accentNavy: Color,
    val onAccentNavy: Color,
    val chartSlate: Color,
    val chartRose: Color,
    val warningAmber: Color,
    val isDark: Boolean
)

val LightExtendedColors = SereneExtendedColors(
    cardBackground = SereneSurfaceLowest,
    cardSubtleBackground = SereneSurfaceLow,
    cardContainer = SereneSurfaceContainer,
    cardBorder = SereneOutlineVariant,
    heroCardBackground = SerenePrimary,
    heroCardAccentCircle = SerenePrimaryContainer,
    heroCardOnBackground = SereneOnPrimary,
    heroCardSubtext = SereneOnPrimaryContainer,
    incomeColor = SereneSecondary,
    incomeContainer = Color(0x1A006C4A),
    expenseColor = SereneError,
    expenseContainer = Color(0x66FFDAD6),
    accentNavy = SerenePrimary,
    onAccentNavy = SereneOnPrimary,
    chartSlate = SereneSurfaceTint,
    chartRose = SereneOnTertiaryContainer,
    warningAmber = Color(0xFFD97706),
    isDark = false
)

val DarkExtendedColors = SereneExtendedColors(
    cardBackground = DarkSurfaceLowest,
    cardSubtleBackground = DarkSurfaceLow,
    cardContainer = DarkSurfaceContainer,
    cardBorder = DarkOutlineVariant,
    heroCardBackground = Color(0xFF0F1B2E),
    heroCardAccentCircle = Color(0xFF1E3252),
    heroCardOnBackground = Color(0xFFF8FAFC),
    heroCardSubtext = Color(0xFF94A3B8),
    incomeColor = DarkSecondary,
    incomeContainer = Color(0x2634D399),
    expenseColor = DarkRoseAccent,
    expenseContainer = Color(0x2EFB7185),
    accentNavy = Color(0xFF1E293B),
    onAccentNavy = Color(0xFFF8FAFC),
    chartSlate = Color(0xFF64748B),
    chartRose = DarkRoseAccent,
    warningAmber = Color(0xFFFBBF24),
    isDark = true
)

val LocalSereneColors = staticCompositionLocalOf { LightExtendedColors }

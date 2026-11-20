package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val SereneLightColorScheme = lightColorScheme(
    primary = SerenePrimary,
    onPrimary = SereneOnPrimary,
    primaryContainer = SerenePrimaryContainer,
    onPrimaryContainer = SereneOnPrimaryContainer,
    secondary = SereneSecondary,
    onSecondary = SereneOnSecondary,
    secondaryContainer = SereneSecondaryContainer,
    onSecondaryContainer = SereneOnSecondaryContainer,
    tertiary = SereneTertiary,
    onTertiary = SereneOnTertiary,
    tertiaryContainer = SereneTertiaryContainer,
    onTertiaryContainer = SereneOnTertiaryContainer,
    error = SereneError,
    onError = SereneOnError,
    errorContainer = SereneErrorContainer,
    onErrorContainer = SereneOnErrorContainer,
    background = SereneSurface,
    onBackground = SereneOnSurface,
    surface = SereneSurface,
    onSurface = SereneOnSurface,
    surfaceVariant = SereneSurfaceHighest,
    onSurfaceVariant = SereneOnSurfaceVariant,
    surfaceContainerLowest = SereneSurfaceLowest,
    surfaceContainerLow = SereneSurfaceLow,
    surfaceContainer = SereneSurfaceContainer,
    surfaceContainerHigh = SereneSurfaceHigh,
    surfaceContainerHighest = SereneSurfaceHighest,
    outline = SereneOutline,
    outlineVariant = SereneOutlineVariant
)

private val SereneDarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkRoseAccent,
    onTertiary = SereneTertiary,
    tertiaryContainer = SereneTertiaryContainer,
    onTertiaryContainer = DarkRoseAccent,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    background = DarkSurface,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceHighest,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLowest = DarkSurfaceLowest,
    surfaceContainerLow = DarkSurfaceLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceHigh,
    surfaceContainerHighest = DarkSurfaceHighest,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant
)

val SereneShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun MizanityTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SereneDarkColorScheme else SereneLightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    CompositionLocalProvider(LocalSereneColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MizanityTypography,
            shapes = SereneShapes,
            content = content
        )
    }
}

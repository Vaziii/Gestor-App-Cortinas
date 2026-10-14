package com.example.cortinas.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration

// ── Material 3 Expressive — Color Schemes ────────────────────────────────────

private val LightColorScheme = lightColorScheme(
    primary                = PrimaryExpressive,
    onPrimary              = OnPrimary,
    primaryContainer       = PrimaryContainer,
    onPrimaryContainer     = OnPrimaryContainer,
    secondary              = Secondary,
    onSecondary            = OnSecondary,
    secondaryContainer     = SecondaryContainer,
    onSecondaryContainer   = OnSecondaryContainer,
    tertiary               = Tertiary,
    onTertiary             = OnTertiary,
    tertiaryContainer      = TertiaryContainer,
    onTertiaryContainer    = OnTertiaryContainer,
    error                  = Error,
    onError                = OnError,
    errorContainer         = ErrorContainer,
    onErrorContainer       = OnErrorContainer,
    background             = BackgroundLight,
    onBackground           = OnBackgroundLight,
    surface                = SurfaceLight,
    onSurface              = OnSurfaceLight,
    surfaceVariant         = SurfaceVariantLight,
    onSurfaceVariant       = OnSurfaceVariantLight,
    outline                = OutlineLight,
    outlineVariant         = OutlineVariantLight,
    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow    = SurfaceContainerLow,
    surfaceContainer       = SurfaceContainer,
    surfaceContainerHigh   = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
)

private val DarkColorScheme = darkColorScheme(
    primary                = PrimaryExpressiveDark,
    onPrimary              = OnPrimaryDark,
    primaryContainer       = PrimaryContainerDark,
    onPrimaryContainer     = OnPrimaryContainerDark,
    secondary              = SecondaryDark,
    onSecondary            = OnSecondaryDark,
    secondaryContainer     = SecondaryContainerDark,
    onSecondaryContainer   = OnSecondaryContainerDark,
    tertiary               = TertiaryDark,
    onTertiary             = OnTertiaryDark,
    tertiaryContainer      = TertiaryContainerDark,
    onTertiaryContainer    = OnTertiaryContainerDark,
    error                  = ErrorDark,
    onError                = OnErrorDark,
    errorContainer         = ErrorContainerDark,
    onErrorContainer       = OnErrorContainerDark,
    background             = BackgroundDark,
    onBackground           = OnBackgroundDark,
    surface                = SurfaceDark,
    onSurface              = OnSurfaceDark,
    surfaceVariant         = SurfaceVariantDark,
    onSurfaceVariant       = OnSurfaceVariantDark,
    outline                = OutlineDark,
    outlineVariant         = OutlineVariantDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow    = SurfaceContainerLowDark,
    surfaceContainer       = SurfaceContainerDark2,
    surfaceContainerHigh   = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
)

// ── M3 Expressive Shapes — radios más generosos y expresivos ─────────────────
private fun cortinasShapes(screenWidthDp: Int): Shapes {
    val compact  = screenWidthDp < 360
    val expanded = screenWidthDp >= 840
    return Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small      = RoundedCornerShape(if (compact) 12.dp else 16.dp),
        medium     = RoundedCornerShape(if (compact) 16.dp else if (expanded) 20.dp else 18.dp),
        large      = RoundedCornerShape(if (compact) 20.dp else if (expanded) 28.dp else 24.dp),
        extraLarge = RoundedCornerShape(if (compact) 28.dp else if (expanded) 36.dp else 32.dp),
    )
}

// ── Tema principal ────────────────────────────────────────────────────────────
@Composable
fun CortinasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme    = if (darkTheme) DarkColorScheme else LightColorScheme
    val screenWidthDp  = LocalConfiguration.current.screenWidthDp

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        shapes      = cortinasShapes(screenWidthDp),
        content     = content
    )
}

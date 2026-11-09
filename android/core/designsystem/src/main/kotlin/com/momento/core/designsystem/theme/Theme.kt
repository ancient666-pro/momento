package com.momento.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val MomentoShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private val MomentoColorScheme = lightColorScheme(
    primary = PrimaryYellow,
    onPrimary = TextDark,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = TextDark,
    secondary = TextPrimary,
    onSecondary = SurfaceWhite,
    secondaryContainer = SurfaceLow,
    onSecondaryContainer = TextPrimary,
    tertiary = TertiaryAccent,
    background = MainBackground,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceLow,
    onSurfaceVariant = TextSecondary,
    outline = BorderColor,
    outlineVariant = SurfaceHigh,
    error = ErrorRed,
    onError = SurfaceWhite,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = ErrorRed
)

@Composable
fun MomentoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MomentoColorScheme,
        typography = MomentoTypography,
        shapes = MomentoShapes,
        content = content
    )
}

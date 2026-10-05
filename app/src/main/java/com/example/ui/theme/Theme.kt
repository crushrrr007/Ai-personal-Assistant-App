package com.example.ui.theme

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
import androidx.compose.ui.unit.dp

@Immutable
data class ExtendedColors(
    val aiTint: Color,
    val onAiTint: Color,
    val deadlineBadge: Color,
    val deadlineBadgeContainer: Color
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        aiTint = LightAiTint,
        onAiTint = LightOnAiTint,
        deadlineBadge = DeadlineBadgeLight,
        deadlineBadgeContainer = DeadlineBadgeContainerLight
    )
}

// 8dp grid, 12dp card corners, 16dp sheet corners
val AssistantShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

private val LightColorScheme = lightColorScheme(
    primary = LightAccent,
    onPrimary = LightOnAccent,
    primaryContainer = LightAiTint,
    onPrimaryContainer = LightOnAiTint,
    background = LightBackground,
    onBackground = LightText,
    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = LightDivider,
    onSurfaceVariant = LightMutedText,
    outline = LightMutedText,
    outlineVariant = LightDivider
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkAccent,
    onPrimary = DarkOnAccent,
    primaryContainer = DarkAiTint,
    onPrimaryContainer = DarkOnAiTint,
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkDivider,
    onSurfaceVariant = DarkMutedText,
    outline = DarkMutedText,
    outlineVariant = DarkDivider
)

@Composable
fun AssistantTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) {
        ExtendedColors(
            aiTint = DarkAiTint,
            onAiTint = DarkOnAiTint,
            deadlineBadge = DeadlineBadgeDark,
            deadlineBadgeContainer = DeadlineBadgeContainerDark
        )
    } else {
        ExtendedColors(
            aiTint = LightAiTint,
            onAiTint = LightOnAiTint,
            deadlineBadge = DeadlineBadgeLight,
            deadlineBadgeContainer = DeadlineBadgeContainerLight
        )
    }

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AssistantShapes,
            content = content
        )
    }
}

val MaterialTheme.extendedColors: ExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtendedColors.current

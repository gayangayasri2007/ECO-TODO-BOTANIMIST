package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class TimelineCustomTokens(
    val timelineLine: Color,
    val nodeActive: Color,
    val nodeCompleted: Color,
    val currentTimeLine: Color,
    val cardBorder: Color,
    val accentGlow: Color = SageLight
)

val LocalTimelineTokens = staticCompositionLocalOf {
    TimelineCustomTokens(
        timelineLine = EarthySageTimelineLine,
        nodeActive = EarthySageTimelineNodeActive,
        nodeCompleted = EarthySageTimelineNodeCompleted,
        currentTimeLine = EarthySageTimelineCurrentTime,
        cardBorder = EarthySageTimelineCardBorder,
        accentGlow = EarthySageTimelineAccentGlow
    )
}

fun getEarthySageColorScheme(isDark: Boolean): Pair<ColorScheme, TimelineCustomTokens> {
    return if (isDark) {
        Pair(
            darkColorScheme(
                primary = EarthySageDarkPrimary,
                secondary = EarthySageDarkSecondary,
                tertiary = EarthySageDarkTertiary,
                background = EarthySageDarkBackground,
                surface = EarthySageDarkSurface,
                surfaceVariant = EarthySageDarkSurfaceVariant,
                onPrimary = EarthySageDarkOnPrimary,
                onSecondary = EarthySageDarkOnSecondary,
                onTertiary = EarthySageDarkOnTertiary,
                onBackground = EarthySageDarkOnBackground,
                onSurface = EarthySageDarkOnSurface,
                onSurfaceVariant = EarthySageDarkOnSurfaceVariant,
                outline = EarthySageDarkOutline,
                outlineVariant = EarthySageDarkOutlineVariant,
                primaryContainer = EarthySageDarkPrimaryContainer,
                secondaryContainer = EarthySageDarkSecondaryContainer
            ),
            TimelineCustomTokens(
                timelineLine = EarthySageDarkTimelineLine,
                nodeActive = EarthySageDarkTimelineNodeActive,
                nodeCompleted = EarthySageDarkTimelineNodeCompleted,
                currentTimeLine = EarthySageDarkTimelineCurrentTime,
                cardBorder = EarthySageDarkTimelineCardBorder,
                accentGlow = EarthySageDarkTimelineAccentGlow
            )
        )
    } else {
        Pair(
            lightColorScheme(
                primary = EarthySagePrimary,
                secondary = EarthySageSecondary,
                tertiary = EarthySageTertiary,
                background = EarthySageBackground,
                surface = EarthySageSurface,
                surfaceVariant = EarthySageSurfaceVariant,
                onPrimary = EarthySageOnPrimary,
                onSecondary = EarthySageOnSecondary,
                onTertiary = EarthySageOnTertiary,
                onBackground = EarthySageOnBackground,
                onSurface = EarthySageOnSurface,
                onSurfaceVariant = EarthySageOnSurfaceVariant,
                outline = EarthySageOutline,
                outlineVariant = EarthySageOutlineVariant,
                primaryContainer = EarthySagePrimaryContainer,
                secondaryContainer = EarthySageSecondaryContainer
            ),
            TimelineCustomTokens(
                timelineLine = EarthySageTimelineLine,
                nodeActive = EarthySageTimelineNodeActive,
                nodeCompleted = EarthySageTimelineNodeCompleted,
                currentTimeLine = EarthySageTimelineCurrentTime,
                cardBorder = EarthySageTimelineCardBorder,
                accentGlow = EarthySageTimelineAccentGlow
            )
        )
    }
}

fun getThemeColorScheme(
    themeName: String = "Earthy Sage",
    isDark: Boolean = false
): Pair<ColorScheme, TimelineCustomTokens> {
    return getEarthySageColorScheme(isDark)
}

@Composable
fun MyApplicationTheme(
    isDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val (colorScheme, timelineTokens) = getEarthySageColorScheme(isDark)

    CompositionLocalProvider(LocalTimelineTokens provides timelineTokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = ExpressiveMaterialShapes,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    themeName: String = "Earthy Sage",
    themeMode: String = "LIGHT",
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> systemDark
    }
    MyApplicationTheme(isDark = isDark, content = content)
}

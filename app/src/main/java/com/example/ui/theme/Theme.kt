package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Color(0xFF00363D),
    primaryContainer = SlateSurfaceVariant,
    onPrimaryContainer = ElectricCyan,
    secondary = QuestGold,
    onSecondary = Color(0xFF3F2E00),
    secondaryContainer = Color(0xFF3B2E15),
    onSecondaryContainer = QuestGold,
    tertiary = CyberPurple,
    background = SlateBackground,
    onBackground = TextPrimaryDark,
    surface = SlateSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    error = Color(0xFFFF5252),
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = Color(0xFFD97706),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFFB45309),
    tertiary = Color(0xFF7C3AED),
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    error = Color(0xFFDC2626),
    onError = Color.White
)

@Composable
fun FocusQuestTheme(
    darkTheme: Boolean = true,
    themeId: String = "theme_deep_slate",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val baseScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> {
            when (themeId) {
                "theme_cyber_neon" -> DarkColorScheme.copy(
                    primary = ThemePalettes.CyberNeonPrimary,
                    secondary = ThemePalettes.CyberNeonSecondary,
                    surface = ThemePalettes.CyberNeonSurface
                )
                "theme_emerald_flow" -> DarkColorScheme.copy(
                    primary = ThemePalettes.EmeraldFlowPrimary,
                    secondary = ThemePalettes.EmeraldFlowSecondary,
                    surface = ThemePalettes.EmeraldFlowSurface
                )
                "theme_solar_gold" -> DarkColorScheme.copy(
                    primary = ThemePalettes.SolarGoldPrimary,
                    secondary = ThemePalettes.SolarGoldSecondary,
                    surface = ThemePalettes.SolarGoldSurface
                )
                else -> DarkColorScheme
            }
        }
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = baseScheme,
        typography = Typography,
        content = content
    )
}

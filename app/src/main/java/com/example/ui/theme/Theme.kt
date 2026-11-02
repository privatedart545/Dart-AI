package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

fun getDartAiColorScheme(themeName: String, darkTheme: Boolean): ColorScheme {
    return if (darkTheme) {
        when (themeName.lowercase()) {
            "emerald" -> darkColorScheme(
                primary = Color(0xFF00FF66),
                onPrimary = CyberBackground,
                primaryContainer = CyberSurfaceElevated,
                onPrimaryContainer = Color(0xFF00FF66),
                secondary = Color(0xFF10B981),
                onSecondary = TextPrimary,
                secondaryContainer = Color(0xFF065F46),
                onSecondaryContainer = TextPrimary,
                tertiary = Color(0xFF34D399),
                background = CyberBackground,
                onBackground = TextPrimary,
                surface = CyberSurface,
                onSurface = TextPrimary,
                surfaceVariant = CyberSurfaceElevated,
                onSurfaceVariant = TextSecondary,
                outline = CyberBorder,
                error = StatusError,
                onError = TextPrimary
            )
            "magenta" -> darkColorScheme(
                primary = Color(0xFFFF007F),
                onPrimary = CyberBackground,
                primaryContainer = CyberSurfaceElevated,
                onPrimaryContainer = Color(0xFFFF007F),
                secondary = Color(0xFFA855F7),
                onSecondary = TextPrimary,
                secondaryContainer = Color(0xFF701A75),
                onSecondaryContainer = TextPrimary,
                tertiary = Color(0xFFF43F5E),
                background = CyberBackground,
                onBackground = TextPrimary,
                surface = CyberSurface,
                onSurface = TextPrimary,
                surfaceVariant = CyberSurfaceElevated,
                onSurfaceVariant = TextSecondary,
                outline = CyberBorder,
                error = StatusError,
                onError = TextPrimary
            )
            "gold" -> darkColorScheme(
                primary = Color(0xFFFFB700),
                onPrimary = CyberBackground,
                primaryContainer = CyberSurfaceElevated,
                onPrimaryContainer = Color(0xFFFFB700),
                secondary = Color(0xFFF97316),
                onSecondary = TextPrimary,
                secondaryContainer = Color(0xFF9A3412),
                onSecondaryContainer = TextPrimary,
                tertiary = Color(0xFFFDE047),
                background = CyberBackground,
                onBackground = TextPrimary,
                surface = CyberSurface,
                onSurface = TextPrimary,
                surfaceVariant = CyberSurfaceElevated,
                onSurfaceVariant = TextSecondary,
                outline = CyberBorder,
                error = StatusError,
                onError = TextPrimary
            )
            else -> darkColorScheme(
                primary = CyanPrimary,
                onPrimary = CyberBackground,
                primaryContainer = CyberSurfaceElevated,
                onPrimaryContainer = CyanPrimary,
                secondary = PurpleAccent,
                onSecondary = TextPrimary,
                secondaryContainer = PurpleDeep,
                onSecondaryContainer = TextPrimary,
                tertiary = CyanVariant,
                background = CyberBackground,
                onBackground = TextPrimary,
                surface = CyberSurface,
                onSurface = TextPrimary,
                surfaceVariant = CyberSurfaceElevated,
                onSurfaceVariant = TextSecondary,
                outline = CyberBorder,
                error = StatusError,
                onError = TextPrimary
            )
        }
    } else {
        when (themeName.lowercase()) {
            "emerald" -> lightColorScheme(
                primary = Color(0xFF059669),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFD1FAE5),
                onPrimaryContainer = Color(0xFF065F46),
                secondary = Color(0xFF10B981),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFA7F3D0),
                onSecondaryContainer = Color(0xFF064E3B),
                tertiary = Color(0xFF047857),
                background = LightBackground,
                onBackground = LightTextPrimary,
                surface = LightSurface,
                onSurface = LightTextPrimary,
                surfaceVariant = LightSurfaceElevated,
                onSurfaceVariant = LightTextSecondary,
                outline = LightBorder,
                error = StatusError,
                onError = Color.White
            )
            "magenta" -> lightColorScheme(
                primary = Color(0xFFC026D3),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFFDF4FF),
                onPrimaryContainer = Color(0xFF701A75),
                secondary = Color(0xFFA855F7),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFF3E8FF),
                onSecondaryContainer = Color(0xFF581C87),
                tertiary = Color(0xFFE11D48),
                background = LightBackground,
                onBackground = LightTextPrimary,
                surface = LightSurface,
                onSurface = LightTextPrimary,
                surfaceVariant = LightSurfaceElevated,
                onSurfaceVariant = LightTextSecondary,
                outline = LightBorder,
                error = StatusError,
                onError = Color.White
            )
            "gold" -> lightColorScheme(
                primary = Color(0xFFD97706),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFFEF3C7),
                onPrimaryContainer = Color(0xFF78350F),
                secondary = Color(0xFFEA580C),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFFFEDD5),
                onSecondaryContainer = Color(0xFF7C2D12),
                tertiary = Color(0xFFCA8A04),
                background = LightBackground,
                onBackground = LightTextPrimary,
                surface = LightSurface,
                onSurface = LightTextPrimary,
                surfaceVariant = LightSurfaceElevated,
                onSurfaceVariant = LightTextSecondary,
                outline = LightBorder,
                error = StatusError,
                onError = Color.White
            )
            else -> lightColorScheme(
                primary = CyanPrimaryLight,
                onPrimary = Color.White,
                primaryContainer = Color(0xFFE0F2FE),
                onPrimaryContainer = Color(0xFF0369A1),
                secondary = PurpleAccentLight,
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFEDE9FE),
                onSecondaryContainer = Color(0xFF5B21B6),
                tertiary = CyanVariant,
                background = LightBackground,
                onBackground = LightTextPrimary,
                surface = LightSurface,
                onSurface = LightTextPrimary,
                surfaceVariant = LightSurfaceElevated,
                onSurfaceVariant = LightTextSecondary,
                outline = LightBorder,
                error = StatusError,
                onError = Color.White
            )
        }
    }
}

// Backward compatibility alias
fun getDevAiColorScheme(themeName: String) = getDartAiColorScheme(themeName, darkTheme = true)

@Composable
fun DevAiTheme(
    themeName: String = "cyan",
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = getDartAiColorScheme(themeName, darkTheme)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

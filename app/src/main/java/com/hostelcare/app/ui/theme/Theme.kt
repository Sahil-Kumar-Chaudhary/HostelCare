package com.hostelcare.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

val PrimaryBlue = Color(0xFF0056D2)
val BackgroundLight = Color(0xFFF9FAFB)
val SurfaceLight = Color(0xFFFFFFFF)
val TextDark = Color(0xFF1F2937)
val TextMuted = Color(0xFF6B7280)
val SuccessGreen = Color(0xFF10B981)
val WarningOrange = Color(0xFFF59E0B)
val DangerRed = Color(0xFFEF4444)

val LightBlue = Color(0xFFEBF3FF)
val GrayBg = Color(0xFFF3F4F6)
val GreenPillBg = Color(0xFFE1F7E3)
val RedPillBg = Color(0xFFFFEBEB)
val OrangePillBg = Color(0xFFFFF3E0)

// Dark Theme Palette
val PrimaryBlueDark = Color(0xFF3B82F6)
val BackgroundDark = Color(0xFF0F172A)
val SurfaceDark = Color(0xFF1E293B)
val TextLight = Color(0xFFF8FAFC)
val TextMutedDark = Color(0xFF94A3B8)
val SurfaceVariantDark = Color(0xFF334155)
val LightBlueDark = Color(0xFF1E3A8A)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = LightBlue,
    onPrimaryContainer = PrimaryBlue,
    secondary = PrimaryBlue,
    onSecondary = Color.White,
    secondaryContainer = LightBlue,
    onSecondaryContainer = PrimaryBlue,
    background = BackgroundLight,
    onBackground = TextDark,
    surface = SurfaceLight,
    onSurface = TextDark,
    surfaceVariant = GrayBg,
    onSurfaceVariant = TextMuted,
    surfaceTint = Color.White,
    outline = Color(0xFFD1D5DB),
    outlineVariant = Color(0xFFE5E7EB),
    error = DangerRed,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlueDark,
    onPrimary = Color.White,
    primaryContainer = LightBlueDark,
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = PrimaryBlueDark,
    onSecondary = Color.White,
    secondaryContainer = LightBlueDark,
    onSecondaryContainer = Color(0xFFDBEAFE),
    background = BackgroundDark,
    onBackground = TextLight,
    surface = SurfaceDark,
    onSurface = TextLight,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextMutedDark,
    surfaceTint = SurfaceDark,
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF334155),
    error = DangerRed,
    onError = Color.White
)

val Shapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

val Typography = Typography()

@Composable
fun HostelCareTheme(
    darkTheme: Boolean = ThemeManager.isDarkMode.value,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            var context = view.context
            while (context is android.content.ContextWrapper) {
                if (context is Activity) break
                context = context.baseContext
            }
            if (context is Activity) {
                val window = context.window
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.surface.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    MaterialTheme(colorScheme = colorScheme, typography = Typography, shapes = Shapes, content = content)
}

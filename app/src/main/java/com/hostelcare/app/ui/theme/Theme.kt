package com.hostelcare.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
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
fun HostelCareTheme(content: @Composable () -> Unit) {
    val colorScheme = LightColorScheme
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
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            }
        }
    }
    MaterialTheme(colorScheme = colorScheme, typography = Typography, shapes = Shapes, content = content)
}

package com.salesapp.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BrandGreen = Color(0xFF16A34A)
val BrandGreenDark = Color(0xFF15803D)
val BrandGreenLight = Color(0xFFDCFCE7)
val BgLight = Color(0xFFF8FAFC)
val Surface1 = Color(0xFFFFFFFF)
val OnSurface = Color(0xFF1F2937)
val Muted = Color(0xFF6B7280)
val Border = Color(0xFFE5E7EB)
val Danger = Color(0xFFEF4444)
val WarningOrange = Color(0xFFF97316)
val Pink = Color(0xFFEC4899)

private val LightScheme = lightColorScheme(
    primary = BrandGreen,
    onPrimary = Color.White,
    primaryContainer = BrandGreenLight,
    onPrimaryContainer = BrandGreenDark,
    secondary = WarningOrange,
    background = BgLight,
    onBackground = OnSurface,
    surface = Surface1,
    onSurface = OnSurface,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Muted,
    error = Danger,
    outline = Border
)

private val DarkScheme = darkColorScheme(
    primary = BrandGreen,
    onPrimary = Color.White,
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    onSurface = Color.White,
    outline = Color(0xFF334155)
)

@Composable
fun SalesAppTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = AppTypography,
        content = content
    )
}

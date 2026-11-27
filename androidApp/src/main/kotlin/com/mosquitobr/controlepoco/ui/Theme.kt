package com.mosquitobr.controlepoco.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Ink = Color(0xFF071016)
val Bg = Color(0xFF0B1117)
val Panel = Color(0xFF131C24)
val Panel2 = Color(0xFF1A2732)
val Panel3 = Color(0xFF223240)
val TextMain = Color(0xFFE6EEF6)
val Muted = Color(0xFF8FA4B6)
val Teal = Color(0xFF2DD4BF)
val TealDark = Color(0xFF0F766E)
val Amber = Color(0xFFF5A524)
val Red = Color(0xFFEF4444)
val Green = Color(0xFF22C55E)
val Blue = Color(0xFF60A5FA)
val Purple = Color(0xFFC084FC)

private val scheme = darkColorScheme(
    primary = Teal,
    onPrimary = Ink,
    primaryContainer = TealDark,
    onPrimaryContainer = TextMain,
    secondary = Amber,
    onSecondary = Ink,
    secondaryContainer = Color(0xFF4A3510),
    onSecondaryContainer = TextMain,
    tertiary = Blue,
    background = Bg,
    onBackground = TextMain,
    surface = Panel,
    onSurface = TextMain,
    surfaceVariant = Panel2,
    onSurfaceVariant = Muted,
    outline = Panel3,
    error = Red,
    onError = Color.White
)

private val typography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 13.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 11.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 0.4.sp
    )
)

@Composable
fun ControlePocoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}

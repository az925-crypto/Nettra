package com.zaaam.nettra.browserui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Android-native 2026 — Expressive + OLED, bukan website paper
object NettraColors {
    val Bg = Color(0xFF0A0E14) // OLED
    val Surface = Color(0xFF121821)
    val Surface2 = Color(0xFF1A232F)
    val Surface3 = Color(0xFF1E2D3D)
    val Text = Color(0xFFE6EDF3)
    val Muted = Color(0xFF8B9AB0)
    val Dim = Color(0xFF5C6B82)
    val Lime = Color(0xFFD6FF2A) // single accent 2026 — signal
    val LimeDim = Color(0xFFE9FF8A)
    val Burn = Color(0xFFFF3B30)
    val Border = Color(0x1FFFFFFF)
}

private val NettraTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 22.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, color = NettraColors.Muted),
    labelSmall = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.8.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.6.sp)
)

private val NettraColorScheme = darkColorScheme(
    background = NettraColors.Bg,
    surface = NettraColors.Bg,
    surfaceContainer = NettraColors.Surface,
    surfaceContainerHigh = NettraColors.Surface2,
    primary = NettraColors.Lime,
    onPrimary = NettraColors.Bg,
    onBackground = NettraColors.Text,
    onSurface = NettraColors.Text,
    onSurfaceVariant = NettraColors.Muted,
    outlineVariant = NettraColors.Border,
    error = NettraColors.Burn
)

@Composable
fun NettraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NettraColorScheme,
        typography = NettraTypography,
        shapes = MaterialTheme.shapes.copy(
            extraLarge = RoundedCornerShape(24.dp),
            large = RoundedCornerShape(16.dp),
            medium = RoundedCornerShape(12.dp),
            small = RoundedCornerShape(8.dp)
        ),
        content = content
    )
}

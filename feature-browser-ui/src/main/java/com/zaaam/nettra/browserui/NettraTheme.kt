package com.zaaam.nettra.browserui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Colors — 1:1 dari mockup/index.html :root
val NettraBg = Color(0xFF0A0E14)
val NettraSurface = Color(0xFF121821)
val NettraSurface2 = Color(0xFF1A232F)
val NettraSurface3 = Color(0xFF1E2D3D)
val NettraBorder = Color(0xFF233041)
val NettraText = Color(0xFFE6EDF3)
val NettraMuted = Color(0xFF8B9AB0)
val NettraDim = Color(0xFF5C6B82)
val NettraFire = Color(0xFFFF4D1A)
val NettraFire2 = Color(0xFFFF7A1A)
val NettraGreen = Color(0xFF1DD75B)
val NettraYellow = Color(0xFFFFB020)
val NettraRed = Color(0xFFFF3B3B)
val NettraPurple = Color(0xFF8B5CF6)
val NettraBlue = Color(0xFF3B82F6)

private val NettraColorScheme = darkColorScheme(
    primary = NettraFire,
    secondary = NettraPurple,
    tertiary = NettraGreen,
    background = NettraBg,
    surface = NettraSurface,
    surfaceVariant = NettraSurface2,
    onBackground = NettraText,
    onSurface = NettraText,
    outline = NettraBorder
)

@Composable
fun NettraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NettraColorScheme,
        typography = Typography(),
        shapes = MaterialTheme.shapes.copy(
            extraLarge = RoundedCornerShape(22.dp),
            large = RoundedCornerShape(16.dp),
            medium = RoundedCornerShape(10.dp)
        ),
        content = content
    )
}

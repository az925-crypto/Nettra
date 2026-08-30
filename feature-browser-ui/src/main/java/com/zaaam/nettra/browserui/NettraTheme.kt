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
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// BUNKER HUD — Tactical OLED (Android native, not web paper)
object NettraColors {
    val VoidBlack = Color(0xFF06080B) // 60% true OLED
    val Bunker = Color(0xFF1A1D21) // surface
    val BunkerRaised = Color(0xFF23262D) // surfaceContainerHigh
    val GhostAsh = Color(0xFF9AA0A8) // onSurfaceVariant
    val GhostWhite = Color(0xFFF2F3F5) // onSurface
    val SignalLime = Color(0xFFD6FF2A) // 10% single hue — secure signal
    val SignalLimeDim = Color(0xFFE9FF8A)
    val AlertEmber = Color(0xFFFF3B30) // destructive <2%
    val Border = Color(0x1FFFFFFF) // 12% white
    val BorderStrong = Color(0xFF2A3036)
}

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = emptyList()
)
private val SpaceGrotesk = GoogleFont("Space Grotesk")
private val InstrumentSans = GoogleFont("Instrument Sans")
private val JetBrainsMono = GoogleFont("JetBrains Mono")

val NettraFontSpace = FontFamily(
    Font(googleFont = SpaceGrotesk, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = SpaceGrotesk, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = SpaceGrotesk, fontProvider = provider)
)
val NettraFontInstrument = FontFamily(
    Font(googleFont = InstrumentSans, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = InstrumentSans, fontProvider = provider)
)
val NettraFontMono = FontFamily(
    Font(googleFont = JetBrainsMono, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = JetBrainsMono, fontProvider = provider, weight = FontWeight.Bold)
)

private val NettraTypography = Typography(
    displayLarge = TextStyle(fontFamily = NettraFontSpace, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.5).sp),
    displayMedium = TextStyle(fontFamily = NettraFontSpace, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 26.sp),
    titleLarge = TextStyle(fontFamily = NettraFontInstrument, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 22.sp),
    titleMedium = TextStyle(fontFamily = NettraFontInstrument, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = NettraFontInstrument, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = NettraFontInstrument, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp, color = NettraColors.GhostAsh),
    labelSmall = TextStyle(fontFamily = NettraFontMono, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.8.sp),
    labelMedium = TextStyle(fontFamily = NettraFontMono, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.2.sp)
)

private val NettraColorScheme = darkColorScheme(
    background = NettraColors.VoidBlack,
    surface = NettraColors.VoidBlack,
    surfaceContainer = NettraColors.Bunker,
    surfaceContainerHigh = NettraColors.BunkerRaised,
    primary = NettraColors.SignalLime,
    onPrimary = NettraColors.VoidBlack,
    onBackground = NettraColors.GhostWhite,
    onSurface = NettraColors.GhostWhite,
    onSurfaceVariant = NettraColors.GhostAsh,
    outlineVariant = NettraColors.Border,
    secondary = NettraColors.GhostAsh,
    error = NettraColors.AlertEmber
)

@Composable
fun NettraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NettraColorScheme,
        typography = NettraTypography,
        shapes = MaterialTheme.shapes.copy(
            extraLarge = RoundedCornerShape(32.dp),
            large = RoundedCornerShape(24.dp),
            medium = RoundedCornerShape(14.dp),
            small = RoundedCornerShape(12.dp)
        ),
        content = content
    )
}

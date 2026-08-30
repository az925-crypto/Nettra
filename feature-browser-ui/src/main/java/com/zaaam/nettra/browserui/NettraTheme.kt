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

// Burn Dossier Palette — THE BURN DOSSIER v2.0 (60/30/10, single hue Vermillion)
object NettraColors {
    val VoidInk = Color(0xFF0B0F12) // 60% dominant — matte archive void
    val Concrete = Color(0xFF1E2328) // surface 1 — concrete
    val ConcreteElevated = Color(0xFF252A30) // surface 2
    val PaperBone = Color(0xFFF2EFE7) // 30% — dossier paper, card & primary on dark
    val Soot = Color(0xFF8C877F) // muted — ash metadata
    val BurnVermillion = Color(0xFFFF3A1E) // 10% sharp accent — burn stamp
    val BurnPressed = Color(0xFFD42F16)
    val Border = Color(0xFF2A3036)
    val BorderStrong = Color(0xFF0B0F12)
}

// Distinctive fonts — Instrument Serif + Space Grotesque + JetBrains Mono (not Inter)
private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = emptyList()
)

private val InstrumentSerif = GoogleFont("Instrument Serif")
private val SpaceGrotesque = GoogleFont("Space Grotesque")
private val JetBrainsMono = GoogleFont("JetBrains Mono")

val NettraFontInstrument = FontFamily(Font(googleFont = InstrumentSerif, fontProvider = provider))
val NettraFontSpace = FontFamily(
    Font(googleFont = SpaceGrotesque, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = SpaceGrotesque, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = SpaceGrotesque, fontProvider = provider, weight = FontWeight.Bold)
)
val NettraFontMono = FontFamily(
    Font(googleFont = JetBrainsMono, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = JetBrainsMono, fontProvider = provider, weight = FontWeight.Bold)
)

private val NettraTypography = Typography(
    displayLarge = TextStyle(fontFamily = NettraFontInstrument, fontWeight = FontWeight.Normal, fontSize = 42.sp, lineHeight = 38.sp, letterSpacing = (-0.5).sp),
    displayMedium = TextStyle(fontFamily = NettraFontInstrument, fontWeight = FontWeight.Normal, fontSize = 28.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = NettraFontInstrument, fontWeight = FontWeight.Normal, fontSize = 22.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = NettraFontSpace, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = NettraFontSpace, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontFamily = NettraFontSpace, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp, color = NettraColors.Soot),
    labelSmall = TextStyle(fontFamily = NettraFontMono, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.8.sp),
    labelMedium = TextStyle(fontFamily = NettraFontMono, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.2.sp)
)

private val NettraColorScheme = darkColorScheme(
    background = NettraColors.VoidInk,
    surface = NettraColors.Concrete,
    surfaceVariant = NettraColors.ConcreteElevated,
    onBackground = NettraColors.PaperBone,
    onSurface = NettraColors.PaperBone,
    primary = NettraColors.BurnVermillion,
    onPrimary = NettraColors.PaperBone,
    outline = NettraColors.Border,
    secondary = NettraColors.Soot
)

@Composable
fun NettraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NettraColorScheme,
        typography = NettraTypography,
        shapes = MaterialTheme.shapes.copy(
            extraLarge = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
            large = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(4.dp),
            small = RoundedCornerShape(4.dp)
        ),
        content = content
    )
}

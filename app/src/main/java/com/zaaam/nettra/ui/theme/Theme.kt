package com.zaaam.nettra.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VoidInk = Color(0xFF0B0F14)
private val Ledger = Color(0xFFF2F4F7)
private val Slate = Color(0xFF6B7A90)
private val Amber = Color(0xFFFFC145)
private val Teal = Color(0xFF00C2A8)

private val LightScheme = lightColorScheme(
    primary = VoidInk, onPrimary = Ledger,
    secondary = Slate, tertiary = Amber,
    background = Ledger, surface = Color.White,
    error = Color(0xFFE5484D)
)

@Composable
fun NetTraTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightScheme, content = content)
}

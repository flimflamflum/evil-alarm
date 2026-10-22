package com.evilalarm.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Palette for the dark, frosted-glass look. */
object Glass {
    val Night = Color(0xFF0B0B1A)
    val NightMid = Color(0xFF15102B)
    val Violet = Color(0xFF8B6CFF)
    val Pink = Color(0xFFFF4D8D)
    val Cyan = Color(0xFF3DD6F5)
    val Danger = Color(0xFFFF6B6B)
    val Success = Color(0xFF4ADE80)

    val Fill = Color.White.copy(alpha = 0.07f)
    val FillStrong = Color.White.copy(alpha = 0.14f)
    val Border = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.05f)))
    val AccentBrush = Brush.linearGradient(listOf(Pink, Violet))

    val TextPrimary = Color(0xFFF4F2FF)
    val TextSecondary = Color(0xA6F4F2FF)
    val TextFaint = Color(0x66F4F2FF)
}

@Composable
fun EvilAlarmTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Glass.Pink,
            onPrimary = Color.White,
            secondary = Glass.Violet,
            onSecondary = Color.White,
            background = Glass.Night,
            onBackground = Glass.TextPrimary,
            surface = Glass.NightMid,
            onSurface = Glass.TextPrimary,
            onSurfaceVariant = Glass.TextSecondary,
            error = Glass.Danger,
        ),
        content = content,
    )
}

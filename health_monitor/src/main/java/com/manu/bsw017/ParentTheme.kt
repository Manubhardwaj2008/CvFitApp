package com.manu.bsw017

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BrandNavyDark = Color(0xFF071B2F)
val BrandCyanBright = Color(0xFF00E5FF)
val PageBackground = Color(0xFFF3F7FB)
val TextPrimary = Color(0xFF0F172A)
val TextSecondary = Color(0xFF64748B)

private val LightColorScheme = lightColorScheme(
    primary = BrandNavyDark,
    onPrimary = Color.White,
    secondary = BrandCyanBright,
    onSecondary = BrandNavyDark,
    background = PageBackground,
    onBackground = TextPrimary,
    surface = Color.White,
    onSurface = TextPrimary
)

@Composable
fun ParentTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}

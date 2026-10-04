package com.jpd3.musicfolderplayer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF82B1FF),
    secondary = androidx.compose.ui.graphics.Color(0xFFB0C7FF),
    tertiary = androidx.compose.ui.graphics.Color(0xFFCFBCFF)
)

private val LightColors = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF1E88E5),
    secondary = androidx.compose.ui.graphics.Color(0xFF64B5F6),
    tertiary = androidx.compose.ui.graphics.Color(0xFF8E24AA)
)

@Composable
fun MusicFolderPlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}

package com.jpd3.musicfolderplayer.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PlayerColors = darkColorScheme(
    primary = Color(0xFFA3D977),
    onPrimary = Color(0xFF14210C),
    primaryContainer = Color(0xFF24351C),
    onPrimaryContainer = Color(0xFFD2EDBA),
    secondary = Color(0xFFB5BDB0),
    onSecondary = Color(0xFF171C15),
    secondaryContainer = Color(0xFF282D26),
    onSecondaryContainer = Color(0xFFDEE5D9),
    tertiary = Color(0xFF8FB9AD),
    background = Color(0xFF0C0E0F),
    onBackground = Color(0xFFDFE3DE),
    surface = Color(0xFF151819),
    onSurface = Color(0xFFDFE3DE),
    surfaceVariant = Color(0xFF202526),
    onSurfaceVariant = Color(0xFF9BA59E),
    surfaceContainerLowest = Color(0xFF090B0C),
    surfaceContainerLow = Color(0xFF111415),
    surfaceContainer = Color(0xFF181C1D),
    surfaceContainerHigh = Color(0xFF202526),
    surfaceContainerHighest = Color(0xFF292F30),
    outline = Color(0xFF56605B),
    outlineVariant = Color(0xFF303735),
    error = Color(0xFFFFB4AB)
)

private val PlayerTypography = Typography(
    headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Medium),
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),
    titleMedium = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Medium),
    titleSmall = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 18.sp, letterSpacing = 0.5.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
)

@Composable
fun MusicFolderPlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PlayerColors,
        typography = PlayerTypography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(2.dp),
            small = RoundedCornerShape(4.dp),
            medium = RoundedCornerShape(6.dp),
            large = RoundedCornerShape(8.dp),
            extraLarge = RoundedCornerShape(8.dp)
        ),
        content = content
    )
}

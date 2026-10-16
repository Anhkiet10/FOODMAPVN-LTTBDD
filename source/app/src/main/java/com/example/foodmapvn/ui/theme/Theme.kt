package com.example.foodmapvn.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = BrickRed,
    onPrimary = Color.White,
    primaryContainer = BrickRedSoft,
    onPrimaryContainer = BrickRed,
    secondary = SuccessGreen,
    onSecondary = Color.White,
    secondaryContainer = SuccessGreenSoft,
    onSecondaryContainer = SuccessGreen,
    tertiary = Amber,
    onTertiary = Color.White,
    background = Color.White,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Sand,
    onSurfaceVariant = InkSecondary,
    outline = SandDark,
    outlineVariant = DividerColor,
)

/**
 * Figma hiện chỉ có giao diện sáng nên tạm thời luôn dùng light theme
 * (tắt dynamic color để giữ đúng màu thương hiệu).
 */
@Composable
fun FoodmapvnTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content,
    )
}

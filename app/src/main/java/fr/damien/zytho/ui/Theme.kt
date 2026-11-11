package fr.damien.zytho.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Amber = Color(0xFFE0A030)
val Copper = Color(0xFFC0703A)
val Foam = Color(0xFFF3E6CF)
val Warn = Color(0xFFE57355)

private val ZythoColors = darkColorScheme(
    primary = Amber,
    onPrimary = Color(0xFF2A1A05),
    primaryContainer = Color(0xFF5A3E12),
    onPrimaryContainer = Color(0xFFFFDDA8),
    secondary = Copper,
    onSecondary = Color(0xFF2B1206),
    secondaryContainer = Color(0xFF4E2C17),
    onSecondaryContainer = Color(0xFFFFDBC8),
    tertiary = Color(0xFFB9C98A),
    background = Color(0xFF15110D),
    onBackground = Foam,
    surface = Color(0xFF15110D),
    onSurface = Foam,
    surfaceVariant = Color(0xFF2E251C),
    onSurfaceVariant = Color(0xFFD5C3AE),
    surfaceContainer = Color(0xFF221B14),
    surfaceContainerLow = Color(0xFF1C1611),
    surfaceContainerHigh = Color(0xFF2A2119),
    surfaceContainerHighest = Color(0xFF352A1F),
    outline = Color(0xFF9C8B78),
    outlineVariant = Color(0xFF4F4337),
    error = Warn,
)

@Composable
fun ZythoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ZythoColors, content = content)
}

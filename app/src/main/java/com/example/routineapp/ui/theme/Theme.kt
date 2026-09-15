package com.example.routineapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val RoutiveColorScheme = lightColorScheme(
    primary = Forest,
    onPrimary = Color.White,
    primaryContainer = Mint,
    onPrimaryContainer = ForestDeep,
    secondary = Leaf,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3E8D5),
    onSecondaryContainer = ForestDeep,
    tertiary = Color(0xFF8D6D13),
    onTertiary = Color.White,
    tertiaryContainer = Warm,
    onTertiaryContainer = Color(0xFF2B2100),
    background = Canvas,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Mist,
    onSurfaceVariant = MutedInk,
    surfaceContainer = Color(0xFFD8E6D8),
    surfaceContainerLow = Color(0xFFEEF5EC),
    surfaceContainerHigh = Color(0xFFCDDDCE),
    outline = Hairline,
    outlineVariant = Color(0xFFD3DED3),
    error = Color(0xFFBA1A1A)
)

private val RoutiveShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun RoutineAppTheme(
    darkTheme: Boolean = false,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RoutiveColorScheme,
        typography = Typography,
        shapes = RoutiveShapes,
        content = content
    )
}

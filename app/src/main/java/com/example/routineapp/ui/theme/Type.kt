package com.example.routineapp.ui.theme

import com.example.routineapp.R
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val RoutiveFont = FontFamily(Font(R.font.nunito_sans))

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = RoutiveFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(fontFamily = RoutiveFont, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontFamily = RoutiveFont, fontWeight = FontWeight.SemiBold),
    headlineMedium = TextStyle(fontFamily = RoutiveFont, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontFamily = RoutiveFont, fontWeight = FontWeight.Bold),
    labelLarge = TextStyle(fontFamily = RoutiveFont, fontWeight = FontWeight.SemiBold),
    bodyMedium = TextStyle(fontFamily = RoutiveFont),
    bodySmall = TextStyle(fontFamily = RoutiveFont),
    labelMedium = TextStyle(fontFamily = RoutiveFont, fontWeight = FontWeight.SemiBold)
)

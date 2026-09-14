package com.example.routineapp.ui.theme

import com.example.routineapp.R
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val RoutiveFont = FontFamily(Font(R.font.nunito_sans))

private fun routiveStyle(
    weight: FontWeight,
    size: Int,
    lineHeight: Int,
    letterSpacing: Float = 0f
) = TextStyle(
    fontFamily = RoutiveFont,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp
)

val Typography = Typography(
    displaySmall = routiveStyle(FontWeight.Bold, 36, 42, -0.6f),
    headlineLarge = routiveStyle(FontWeight.Bold, 30, 36, -0.4f),
    headlineMedium = routiveStyle(FontWeight.Bold, 26, 32, -0.25f),
    headlineSmall = routiveStyle(FontWeight.Bold, 22, 28),
    titleLarge = routiveStyle(FontWeight.Bold, 20, 26),
    titleMedium = routiveStyle(FontWeight.SemiBold, 17, 22),
    titleSmall = routiveStyle(FontWeight.SemiBold, 15, 20),
    bodyLarge = routiveStyle(FontWeight.Normal, 16, 24),
    bodyMedium = routiveStyle(FontWeight.Normal, 14, 20),
    bodySmall = routiveStyle(FontWeight.Normal, 12, 17),
    labelLarge = routiveStyle(FontWeight.Bold, 14, 18, 0.1f),
    labelMedium = routiveStyle(FontWeight.SemiBold, 12, 16, 0.1f),
    labelSmall = routiveStyle(FontWeight.SemiBold, 10, 14, 0.2f)
)

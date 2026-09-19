package com.example.homework.ui.uikit.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val SerifFamily = FontFamily.Serif

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = SerifFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 44.sp,
        lineHeight = 50.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = SerifFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.3).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = SerifFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = SerifFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.2.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
)

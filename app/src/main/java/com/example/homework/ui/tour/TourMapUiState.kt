package com.example.homework.ui.tour

import androidx.annotation.DrawableRes
import androidx.compose.ui.geometry.Offset
import com.example.homework.R

data class MapStop(
    val title: String,
    val minutes: Int,
    @param:DrawableRes val photoRes: Int,
    val x: Float,
    val y: Float,
    val filledDot: Boolean = true,
)

data class TourMapUiState(
    val distanceMeters: Int,
    val instructionPrimary: String,
    val instructionSecondary: String,
    @param:DrawableRes val instructionPhotoRes: Int,
    val currentTitle: String,
    val currentDescription: String,
    @param:DrawableRes val currentPhotoRes: Int,
    val stepIndex: Int,
    val stepCount: Int,
    val remainingLabel: String,
    val stops: List<MapStop>,
    val user: Offset,
    val route: List<Offset>,
)

val PreviewTourMapState = TourMapUiState(
    distanceMeters = 120,
    instructionPrimary = "Продолжайте прямо",
    instructionSecondary = "по улице Баумана",
    instructionPhotoRes = R.drawable.photo_bauman,
    currentTitle = "Улица Баумана",
    currentDescription = "Главная пешеходная улица Казани.\nИстория, архитектура, атмосфера.",
    currentPhotoRes = R.drawable.photo_bauman,
    stepIndex = 3,
    stepCount = 8,
    remainingLabel = "Осталось ≈ 1 ч 20 мин",
    stops = listOf(
        MapStop("Казанский Кремль", 25, R.drawable.photo_kremlin, 0.20f, 0.168f, filledDot = true),
        MapStop("Мечеть Кул Шариф", 18, R.drawable.photo_kul_sharif, 0.36f, 0.328f, filledDot = false),
        MapStop("Улица Баумана", 7, R.drawable.photo_bauman, 0.42f, 0.498f, filledDot = false),
        MapStop("Старо-Татарская слобода", 12, R.drawable.photo_sloboda, 0.40f, 0.598f, filledDot = true),
    ),
    user = Offset(0.325f, 0.455f),
    route = listOf(
        Offset(0.225f, 0.225f),
        Offset(0.238f, 0.268f),
        Offset(0.255f, 0.318f),
        Offset(0.278f, 0.372f),
        Offset(0.305f, 0.418f),
        Offset(0.325f, 0.455f),
        Offset(0.348f, 0.508f),
        Offset(0.362f, 0.555f),
        Offset(0.368f, 0.605f),
        Offset(0.372f, 0.648f),
    ),
)

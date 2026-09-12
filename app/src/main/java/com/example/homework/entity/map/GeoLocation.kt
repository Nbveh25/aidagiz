package com.example.homework.entity.map

data class GeoLocation(
    val lat: Double,
    val lon: Double,
    val accuracyMeters: Float = 0f,
    val bearing: Float = 0f,
)
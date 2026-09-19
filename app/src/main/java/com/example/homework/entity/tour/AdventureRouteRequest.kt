package com.example.homework.entity.tour

import com.example.homework.entity.map.GeoLocation

data class AdventureRouteRequest(
    val userLocation: GeoLocation,
    val durationMinutes: Int,
    val interests: List<String>,
    val pace: String,
    val requiredPlaces: List<GeoLocation> = emptyList(),
    val visitedPlaces: String? = null,
    val aiRequest: String? = null,
    val startAt: String? = null,
    val optimizeVisitOrder: Boolean = true,
)

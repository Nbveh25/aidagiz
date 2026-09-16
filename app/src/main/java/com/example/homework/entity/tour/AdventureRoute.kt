package com.example.homework.entity.tour

import com.example.homework.entity.map.OsmPlace

data class AdventureRoute(
    val startedAt: String,
    val finishedAt: String,
    val totalDurationMinutes: Int,
    val totalTravelDurationMinutes: Int,
    val totalVisitDurationMinutes: Int,
    val places: List<OsmPlace>,
)

package com.example.homework.entity.tour

import com.example.homework.entity.map.OsmPlace
import com.example.homework.entity.map.RoutePlace

data class AdventureRoute(
    val startedAt: String,
    val finishedAt: String,
    val totalDurationMinutes: Int,
    val totalTravelDurationMinutes: Int,
    val totalVisitDurationMinutes: Int,
    val places: List<OsmPlace>,
    val summary: String? = null,
)

fun AdventureRoute.toRoutePlaces(): List<RoutePlace> =
    places.mapIndexed { index, place ->
        RoutePlace(
            place = place,
            order = place.stopOrder ?: (index + 1),
            visitDurationMinutes = place.visitDurationMinutes
                ?: place.category.defaultVisitMinutes,
            travelDurationMinutes = place.travelDurationMinutes ?: 0,
            arrivalAt = place.arrivalAt,
            departureAt = place.departureAt,
        )
    }

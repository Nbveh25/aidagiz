package com.example.homework.entity.map

data class RoutePlace(
    val place: OsmPlace,
    val order: Int,
    val visitDurationMinutes: Int,
    val travelDurationMinutes: Int = 0,
    val arrivalAt: String? = null,
    val departureAt: String? = null,
) {
    val id: String get() = place.id
    val name: String get() = place.name
    val lat: Double get() = place.lat
    val lon: Double get() = place.lon
    val category: PlaceCategory get() = place.category
    val location: GeoLocation get() = GeoLocation(lat, lon)
}

fun clampVisitDuration(minutes: Int): Int = minutes.coerceIn(5, 180)

fun addRoutePlace(places: List<RoutePlace>, place: OsmPlace): List<RoutePlace> {
    if (places.any { it.id == place.id }) return places
    return places + RoutePlace(
        place = place,
        order = places.size + 1,
        visitDurationMinutes = place.category.defaultVisitMinutes,
    )
}

fun removeRoutePlace(places: List<RoutePlace>, placeId: String): List<RoutePlace> =
    reorderRoutePlaces(places.filter { it.id != placeId })

fun reorderRoutePlaces(places: List<RoutePlace>): List<RoutePlace> =
    places.mapIndexed { index, item -> item.copy(order = index + 1) }

fun updateRoutePlaceVisitDuration(
    places: List<RoutePlace>,
    placeId: String,
    minutes: Int,
): List<RoutePlace> = places.map { item ->
    if (item.id == placeId) item.copy(visitDurationMinutes = clampVisitDuration(minutes)) else item
}

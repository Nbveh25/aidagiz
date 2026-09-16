package com.example.homework.entity.map

data class RouteStep(
    val maneuverType: String,
    val modifier: String?,
    val location: GeoLocation,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val name: String,
)

data class RouteResult(
    val geometry: List<GeoLocation>,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val steps: List<RouteStep>,
    val mode: TransportMode,
)

data class RouteMatrix(
    val durations: List<List<Double?>>,
    val distances: List<List<Double?>>,
)

fun optimizeRoutePlaces(
    places: List<RoutePlace>,
    matrix: RouteMatrix,
): List<RoutePlace> {
    if (places.size <= 1) return reorderRoutePlaces(places)
    val remaining = places.indices.toMutableSet()
    val order = ArrayList<Int>(places.size)
    var currentMatrixIndex = 0
    while (remaining.isNotEmpty()) {
        val next = remaining.minWithOrNull(
            compareBy<Int> { index ->
                matrix.durations.getOrNull(currentMatrixIndex)?.getOrNull(index + 1)
                    ?: Double.POSITIVE_INFINITY
            }.thenBy { it },
        )
        if (next == null) break
        val duration = matrix.durations.getOrNull(currentMatrixIndex)?.getOrNull(next + 1)
        if (duration == null || !duration.isFinite()) {
            remaining.sorted().forEach(order::add)
            break
        }
        remaining.remove(next)
        order += next
        currentMatrixIndex = next + 1
    }
    return reorderRoutePlaces(order.map { places[it] })
}

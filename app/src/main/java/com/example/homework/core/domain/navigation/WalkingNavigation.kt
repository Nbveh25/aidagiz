package com.example.homework.core.domain.navigation

import com.example.homework.entity.map.GeoLocation
import com.example.homework.entity.map.NavigationState
import com.example.homework.entity.map.NavigationStatus
import com.example.homework.entity.map.RoutePlace
import com.example.homework.entity.map.RouteResult
import com.example.homework.entity.map.RouteStep
import com.example.homework.entity.map.distanceMeters
import kotlin.math.cos
import kotlin.math.sqrt

data class NavigationEffect(
    val rebuildRoute: Boolean = false,
)

class WalkingNavigation {
    var state: NavigationState = NavigationState()
        private set

    var places: List<RoutePlace> = emptyList()
        private set

    private var consecutiveArrival = 0
    private var consecutiveOffRoute = 0
    private var lastRebuildAtMs = 0L

    fun start(routePlaces: List<RoutePlace>, location: GeoLocation?): NavigationEffect {
        places = routePlaces
        consecutiveArrival = 0
        consecutiveOffRoute = 0
        lastRebuildAtMs = 0L
        state = NavigationState(
            status = if (location == null) {
                NavigationStatus.WaitingLocation
            } else {
                NavigationStatus.BuildingRoute
            },
            placeIndex = 0,
            followUser = true,
            instruction = "Строим пеший маршрут…",
        )
        return NavigationEffect(rebuildRoute = location != null)
    }

    fun stop() {
        places = emptyList()
        consecutiveArrival = 0
        consecutiveOffRoute = 0
        state = NavigationState()
    }

    fun pause() {
        if (state.status == NavigationStatus.Navigating) {
            state = state.copy(status = NavigationStatus.Paused, message = "Навигация на паузе")
        }
    }

    fun resume() {
        if (state.status == NavigationStatus.Paused) {
            state = state.copy(status = NavigationStatus.Navigating, message = null)
        }
    }

    fun onUserDrag() {
        if (state.followUser) {
            state = state.copy(followUser = false)
        }
    }

    fun enableFollow() {
        state = state.copy(followUser = true)
    }

    fun retry(location: GeoLocation?): NavigationEffect {
        if (location == null) {
            state = state.copy(
                status = NavigationStatus.WaitingLocation,
                message = "Ждём геолокацию",
            )
            return NavigationEffect()
        }
        state = state.copy(
            status = NavigationStatus.BuildingRoute,
            message = null,
            instruction = "Строим пеший маршрут…",
        )
        return NavigationEffect(rebuildRoute = true)
    }

    fun goToNextPlace(location: GeoLocation?): NavigationEffect {
        val nextIndex = state.placeIndex + 1
        if (nextIndex >= places.size) {
            state = state.copy(
                status = NavigationStatus.Finished,
                instruction = "Маршрут завершён",
                message = null,
                followUser = false,
            )
            return NavigationEffect()
        }
        consecutiveArrival = 0
        consecutiveOffRoute = 0
        state = state.copy(
            status = if (location == null) {
                NavigationStatus.WaitingLocation
            } else {
                NavigationStatus.BuildingRoute
            },
            placeIndex = nextIndex,
            stepIndex = 0,
            route = null,
            instruction = "Строим следующий участок…",
            message = null,
        )
        return NavigationEffect(rebuildRoute = location != null)
    }

    fun onRouteBuilt(result: RouteResult) {
        consecutiveOffRoute = 0
        state = state.copy(
            status = NavigationStatus.Navigating,
            route = result,
            stepIndex = 0,
            instruction = result.steps.firstOrNull()?.let(::formatManeuver) ?: "Идите к точке",
            message = null,
        )
    }

    fun onRouteFailed(message: String) {
        state = state.copy(
            status = NavigationStatus.Error,
            message = message,
            instruction = "Не удалось построить маршрут",
        )
    }

    fun onLocation(location: GeoLocation, nowMs: Long): NavigationEffect {
        val current = remainingPlaces().firstOrNull()
        if (state.status == NavigationStatus.WaitingLocation || state.status == NavigationStatus.Error) {
            state = state.copy(
                status = NavigationStatus.BuildingRoute,
                message = null,
                instruction = "Строим пеший маршрут…",
            )
            return NavigationEffect(rebuildRoute = true)
        }
        if (state.status != NavigationStatus.Navigating) return NavigationEffect()
        if (location.accuracyMeters > 100f) {
            state = state.copy(
                accuracyWarning = true,
                message = "Слабый GPS (${location.accuracyMeters.toInt()} м)",
            )
            return NavigationEffect()
        }
        if (current == null) {
            state = state.copy(status = NavigationStatus.Finished, instruction = "Маршрут завершён")
            return NavigationEffect()
        }
        val toPlace = distanceMeters(location.lat, location.lon, current.lat, current.lon)
        val arrivalRadius = maxOf(35, minOf(location.accuracyMeters.toInt(), 60))
        consecutiveArrival = if (toPlace <= arrivalRadius) consecutiveArrival + 1 else 0
        val route = state.route
        val toRoute = route?.let { distanceMetersToRoute(location, it.geometry) }
        var stepIndex = state.stepIndex
        if (route != null && stepIndex < route.steps.lastIndex) {
            val step = route.steps[stepIndex]
            val toStep = distanceMeters(location.lat, location.lon, step.location.lat, step.location.lon)
            if (toStep <= 25) {
                stepIndex += 1
            }
        }
        val offRouteLimit = maxOf(50.0, location.accuracyMeters * 1.5)
        val offRoute = toRoute != null && toRoute > offRouteLimit
        consecutiveOffRoute = if (offRoute) consecutiveOffRoute + 1 else 0
        val instruction = route?.steps?.getOrNull(stepIndex)?.let(::formatManeuver)
            ?: "Идите к ${current.name}"
        if (consecutiveArrival >= 2) {
            state = state.copy(
                status = NavigationStatus.Arrived,
                distanceToPlaceMeters = toPlace,
                distanceToRouteMeters = toRoute,
                accuracyWarning = false,
                instruction = "Вы на месте: ${current.name}",
                message = null,
                stepIndex = stepIndex,
            )
            return NavigationEffect()
        }
        val shouldRebuild = consecutiveOffRoute >= 3 && nowMs - lastRebuildAtMs >= REBUILD_INTERVAL_MS
        if (shouldRebuild) {
            lastRebuildAtMs = nowMs
            consecutiveOffRoute = 0
            state = state.copy(
                status = NavigationStatus.BuildingRoute,
                distanceToPlaceMeters = toPlace,
                distanceToRouteMeters = toRoute,
                accuracyWarning = false,
                instruction = "Вы свернули с маршрута, перестраиваем…",
                message = null,
                stepIndex = stepIndex,
            )
            return NavigationEffect(rebuildRoute = true)
        }
        state = state.copy(
            distanceToPlaceMeters = toPlace,
            distanceToRouteMeters = toRoute,
            accuracyWarning = false,
            instruction = instruction,
            message = if (offRoute) "Сход с маршрута" else null,
            stepIndex = stepIndex,
        )
        return NavigationEffect()
    }

    fun remainingPlaces(): List<RoutePlace> = places.drop(state.placeIndex)

    companion object {
        private const val REBUILD_INTERVAL_MS = 20_000L
    }
}

fun formatManeuver(step: RouteStep): String {
    val modifier = when (step.modifier?.lowercase()) {
        "left" -> "налево"
        "right" -> "направо"
        "slight left" -> "левее"
        "slight right" -> "правее"
        "sharp left" -> "круто налево"
        "sharp right" -> "круто направо"
        "uturn", "u-turn" -> "разворот"
        "straight" -> "прямо"
        else -> null
    }
    val along = step.name.takeIf { it.isNotBlank() }?.let { " по $it" }.orEmpty()
    return when (step.maneuverType.lowercase()) {
        "depart" -> "Начните движение$along"
        "arrive" -> "Вы на месте"
        "continue", "new name" -> "Продолжайте$along"
        "fork" -> "На развилке держитесь ${modifier ?: "основного направления"}"
        "turn", "end of road" -> "Поверните ${modifier ?: "по маршруту"}$along"
        "roundabout", "rotary", "exit roundabout" -> "На круге ${modifier ?: "следуйте по кругу"}"
        else -> if (modifier != null) "Поверните $modifier$along" else "Продолжайте маршрут$along"
    }
}

fun formatNavigationDistance(meters: Double): String {
    val value = meters.coerceAtLeast(0.0)
    return when {
        value >= 1000 -> "%.1f км".format(value / 1000.0)
        value >= 50 -> "${(value / 10).toInt() * 10} м"
        else -> "${(value / 5).toInt() * 5} м"
    }
}

fun distanceMetersToRoute(location: GeoLocation, geometry: List<GeoLocation>): Int {
    if (geometry.isEmpty()) return Int.MAX_VALUE
    if (geometry.size == 1) {
        return distanceMeters(location.lat, location.lon, geometry.first().lat, geometry.first().lon)
    }
    var min = Double.MAX_VALUE
    for (index in 0 until geometry.lastIndex) {
        min = minOf(min, distancePointToSegment(location, geometry[index], geometry[index + 1]))
    }
    return min.toInt()
}

private fun distancePointToSegment(point: GeoLocation, start: GeoLocation, end: GeoLocation): Double {
    val metersPerLat = 110_540.0
    val metersPerLon = 111_320.0 * cos(Math.toRadians(start.lat))
    val bx = (end.lon - start.lon) * metersPerLon
    val by = (end.lat - start.lat) * metersPerLat
    val px = (point.lon - start.lon) * metersPerLon
    val py = (point.lat - start.lat) * metersPerLat
    val length2 = bx * bx + by * by
    val t = if (length2 == 0.0) 0.0 else ((px * bx + py * by) / length2).coerceIn(0.0, 1.0)
    val dx = px - t * bx
    val dy = py - t * by
    return sqrt(dx * dx + dy * dy)
}

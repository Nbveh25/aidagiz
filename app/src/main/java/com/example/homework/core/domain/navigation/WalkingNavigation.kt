package com.example.homework.core.domain.navigation

import com.example.homework.R
import com.example.homework.core.locale.AppStrings
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
    val arrivedAtLastStop: Boolean = false,
)

class WalkingNavigation(
    private val strings: AppStrings,
) {
    var state: NavigationState = NavigationState()
        private set

    var places: List<RoutePlace> = emptyList()
        private set

    private var consecutiveArrival = 0
    private var consecutiveOffRoute = 0
    private var lastRebuildAtMs = 0L
    private var trimmedPlaceIndex: Int? = null
    private var lastStopAnnounced = false

    fun start(routePlaces: List<RoutePlace>, location: GeoLocation?): NavigationEffect {
        places = routePlaces
        consecutiveArrival = 0
        consecutiveOffRoute = 0
        lastRebuildAtMs = 0L
        trimmedPlaceIndex = null
        lastStopAnnounced = false
        state = NavigationState(
            status = if (location == null) {
                NavigationStatus.WaitingLocation
            } else {
                NavigationStatus.BuildingRoute
            },
            placeIndex = 0,
            followUser = true,
            instruction = strings.get(R.string.nav_building_walk),
        )
        return NavigationEffect(rebuildRoute = location != null)
    }

    fun stop() {
        places = emptyList()
        consecutiveArrival = 0
        consecutiveOffRoute = 0
        trimmedPlaceIndex = null
        lastStopAnnounced = false
        state = NavigationState()
    }

    fun pause() {
        if (state.status == NavigationStatus.Navigating) {
            state = state.copy(
                status = NavigationStatus.Paused,
                message = strings.get(R.string.nav_paused_message),
            )
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
                message = strings.get(R.string.nav_waiting_gps),
            )
            return NavigationEffect()
        }
        trimmedPlaceIndex = null
        lastStopAnnounced = false
        state = state.copy(
            status = NavigationStatus.BuildingRoute,
            message = null,
            instruction = strings.get(R.string.nav_building_walk),
        )
        return NavigationEffect(rebuildRoute = true)
    }

    fun goToNextPlace(location: GeoLocation?): NavigationEffect {
        val nextIndex = state.placeIndex + 1
        if (nextIndex >= places.size) {
            state = state.copy(
                status = NavigationStatus.Finished,
                instruction = strings.get(R.string.nav_finished),
                message = null,
                followUser = false,
            )
            return NavigationEffect()
        }
        consecutiveArrival = 0
        consecutiveOffRoute = 0
        trimmedPlaceIndex = null
        lastStopAnnounced = false
        state = state.copy(
            status = if (location == null) {
                NavigationStatus.WaitingLocation
            } else {
                NavigationStatus.BuildingRoute
            },
            placeIndex = nextIndex,
            stepIndex = 0,
            route = null,
            instruction = strings.get(R.string.nav_building_next),
            message = null,
        )
        return NavigationEffect(rebuildRoute = location != null)
    }

    fun onRouteBuilt(result: RouteResult) {
        consecutiveOffRoute = 0
        trimmedPlaceIndex = null
        state = state.copy(
            status = NavigationStatus.Navigating,
            route = result,
            stepIndex = 0,
            instruction = result.steps.firstOrNull()?.let(::formatManeuver)
                ?: strings.get(R.string.nav_go_to_point),
            message = null,
        )
    }

    fun onRouteFailed(message: String) {
        state = state.copy(
            status = NavigationStatus.Error,
            message = message,
            instruction = strings.get(R.string.nav_build_failed),
        )
    }

    fun onLocation(location: GeoLocation, nowMs: Long): NavigationEffect {
        val current = remainingPlaces().firstOrNull()
        if (state.status == NavigationStatus.WaitingLocation || state.status == NavigationStatus.Error) {
            state = state.copy(
                status = NavigationStatus.BuildingRoute,
                message = null,
                instruction = strings.get(R.string.nav_building_walk),
            )
            return NavigationEffect(rebuildRoute = true)
        }
        if (state.status == NavigationStatus.Arrived) {
            if (current != null) {
                val toPlace = distanceMeters(location.lat, location.lon, current.lat, current.lon)
                val trimmed = hideApproachedLine(location, current, state.route)
                val announceLast = remainingPlaces().size <= 1 &&
                    toPlace <= LINE_HIDE_RADIUS_METERS &&
                    !lastStopAnnounced
                if (announceLast) lastStopAnnounced = true
                state = state.copy(
                    route = trimmed,
                    distanceToPlaceMeters = toPlace,
                )
                if (announceLast) return NavigationEffect(arrivedAtLastStop = true)
            }
            return NavigationEffect()
        }
        if (state.status != NavigationStatus.Navigating) return NavigationEffect()
        if (location.accuracyMeters > 100f) {
            state = state.copy(
                accuracyWarning = true,
                message = strings.get(R.string.nav_weak_gps, location.accuracyMeters.toInt()),
            )
            return NavigationEffect()
        }
        if (current == null) {
            state = state.copy(
                status = NavigationStatus.Finished,
                instruction = strings.get(R.string.nav_finished),
            )
            return NavigationEffect()
        }
        val toPlace = distanceMeters(location.lat, location.lon, current.lat, current.lon)
        val route = hideApproachedLine(location, current, state.route)
        if (remainingPlaces().size <= 1 && toPlace <= LINE_HIDE_RADIUS_METERS && !lastStopAnnounced) {
            lastStopAnnounced = true
            state = state.copy(
                status = NavigationStatus.Arrived,
                route = route,
                distanceToPlaceMeters = toPlace,
                accuracyWarning = false,
                instruction = strings.get(R.string.nav_arrive),
                message = null,
            )
            return NavigationEffect(arrivedAtLastStop = true)
        }
        val arrivalRadius = maxOf(35, minOf(location.accuracyMeters.toInt(), 60))
        consecutiveArrival = if (toPlace <= arrivalRadius) consecutiveArrival + 1 else 0
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
            ?: strings.get(R.string.nav_go_to_named, current.name)
        if (consecutiveArrival >= 2) {
            state = state.copy(
                status = NavigationStatus.Arrived,
                route = route,
                distanceToPlaceMeters = toPlace,
                distanceToRouteMeters = toRoute,
                accuracyWarning = false,
                instruction = strings.get(R.string.nav_arrived_named, current.name),
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
                instruction = strings.get(R.string.nav_rerouting),
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
            message = if (offRoute) strings.get(R.string.nav_off_route) else null,
            stepIndex = stepIndex,
        )
        return NavigationEffect()
    }

    fun remainingPlaces(): List<RoutePlace> = places.drop(state.placeIndex)

    private fun hideApproachedLine(
        location: GeoLocation,
        current: RoutePlace,
        route: RouteResult?,
    ): RouteResult? {
        if (route == null) return null
        val toPlace = distanceMeters(location.lat, location.lon, current.lat, current.lon)
        if (toPlace > LINE_HIDE_RADIUS_METERS) return route
        if (trimmedPlaceIndex == state.placeIndex) return route
        trimmedPlaceIndex = state.placeIndex
        return if (remainingPlaces().size <= 1) {
            route.copy(geometry = emptyList(), steps = emptyList())
        } else {
            remainingRouteFrom(current.location, route)
        }
    }

    private fun formatManeuver(step: RouteStep): String {
        val modifier = when (step.modifier?.lowercase()) {
            "left" -> strings.get(R.string.nav_mod_left)
            "right" -> strings.get(R.string.nav_mod_right)
            "slight left" -> strings.get(R.string.nav_mod_slight_left)
            "slight right" -> strings.get(R.string.nav_mod_slight_right)
            "sharp left" -> strings.get(R.string.nav_mod_sharp_left)
            "sharp right" -> strings.get(R.string.nav_mod_sharp_right)
            "uturn", "u-turn" -> strings.get(R.string.nav_mod_uturn)
            "straight" -> strings.get(R.string.nav_mod_straight)
            else -> null
        }
        val along = step.name.takeIf { it.isNotBlank() }
            ?.let { strings.get(R.string.nav_along, it) }
            .orEmpty()
        return when (step.maneuverType.lowercase()) {
            "depart" -> strings.get(R.string.nav_depart, along)
            "arrive" -> strings.get(R.string.nav_arrive)
            "continue", "new name" -> strings.get(R.string.nav_continue, along)
            "fork" -> strings.get(R.string.nav_fork, modifier ?: strings.get(R.string.nav_fork_default))
            "turn", "end of road" -> strings.get(
                R.string.nav_turn,
                modifier ?: strings.get(R.string.nav_turn_default),
                along,
            )
            "roundabout", "rotary", "exit roundabout" -> strings.get(
                R.string.nav_roundabout,
                modifier ?: strings.get(R.string.nav_roundabout_default),
            )
            else -> if (modifier != null) {
                strings.get(R.string.nav_turn_simple, modifier, along)
            } else {
                strings.get(R.string.nav_continue_route, along)
            }
        }
    }

    private companion object {
        const val REBUILD_INTERVAL_MS = 20_000L
        const val LINE_HIDE_RADIUS_METERS = 25
    }
}

private fun remainingRouteFrom(location: GeoLocation, route: RouteResult): RouteResult {
    val geometry = route.geometry
    if (geometry.size < 2) return route.copy(geometry = emptyList())
    var bestIndex = 0
    var bestT = 0.0
    var bestDistance = Double.MAX_VALUE
    for (index in 0 until geometry.lastIndex) {
        val hit = hitOnSegment(location, geometry[index], geometry[index + 1])
        if (hit.distance < bestDistance) {
            bestDistance = hit.distance
            bestIndex = index
            bestT = hit.t
        }
    }
    val cut = interpolate(geometry[bestIndex], geometry[bestIndex + 1], bestT)
    val remaining = ArrayList<GeoLocation>(geometry.size - bestIndex)
    remaining += cut
    for (index in bestIndex + 1 until geometry.size) {
        remaining += geometry[index]
    }
    if (remaining.size < 2) return route.copy(geometry = emptyList())
    return route.copy(geometry = remaining)
}

fun distanceMetersToRoute(location: GeoLocation, geometry: List<GeoLocation>): Int {
    if (geometry.isEmpty()) return Int.MAX_VALUE
    if (geometry.size == 1) {
        return distanceMeters(location.lat, location.lon, geometry.first().lat, geometry.first().lon)
    }
    var min = Double.MAX_VALUE
    for (index in 0 until geometry.lastIndex) {
        min = minOf(min, hitOnSegment(location, geometry[index], geometry[index + 1]).distance)
    }
    return min.toInt()
}

private data class SegmentHit(val distance: Double, val t: Double)

private fun hitOnSegment(point: GeoLocation, start: GeoLocation, end: GeoLocation): SegmentHit {
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
    return SegmentHit(distance = sqrt(dx * dx + dy * dy), t = t)
}

private fun interpolate(start: GeoLocation, end: GeoLocation, t: Double): GeoLocation =
    GeoLocation(
        lat = start.lat + (end.lat - start.lat) * t,
        lon = start.lon + (end.lon - start.lon) * t,
    )

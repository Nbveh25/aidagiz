package com.example.homework.entity.map

enum class NavigationStatus {
    Idle,
    WaitingLocation,
    BuildingRoute,
    Navigating,
    Paused,
    Arrived,
    Finished,
    Error,
}

data class NavigationState(
    val status: NavigationStatus = NavigationStatus.Idle,
    val placeIndex: Int = 0,
    val stepIndex: Int = 0,
    val route: RouteResult? = null,
    val instruction: String = "",
    val distanceToPlaceMeters: Int? = null,
    val distanceToRouteMeters: Int? = null,
    val accuracyWarning: Boolean = false,
    val message: String? = null,
    val followUser: Boolean = true,
) {
    val isActive: Boolean
        get() = status != NavigationStatus.Idle && status != NavigationStatus.Finished
}

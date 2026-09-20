package com.example.homework.entity.tour

import com.example.homework.entity.map.GeoLocation

enum class BuilderView {
    Form,
    PickStart,
    Planner,
}

enum class RouteStatus {
    Idle,
    Loading,
    Success,
    Error,
}

enum class WalkPace(val apiValue: String) {
    Fast("быстрый"),
    Normal("обычный"),
    Leisurely("неспешный"),
    ;

    companion object {
        fun fromApi(value: String): WalkPace =
            entries.firstOrNull { it.apiValue == value } ?: Normal
    }
}

enum class CulturalInterest(val apiValue: String) {
    History("история"),
    TatarCulture("татарская культура"),
    Architecture("архитектура"),
    Museums("музеи"),
    Mosques("мечети"),
    Parks("парки"),
}

data class RouteFormState(
    val durationMinutes: Int = DEFAULT_DURATION_MINUTES,
    val interests: Set<String> = DEFAULT_INTERESTS,
    val pace: WalkPace = WalkPace.Normal,
    val aiRequest: String = "",
) {
    val isValid: Boolean
        get() = durationMinutes in MIN_DURATION_MINUTES..MAX_DURATION_MINUTES &&
            interests.isNotEmpty()

    fun toRequest(
        location: GeoLocation,
        startAt: String? = null,
    ): AdventureRouteRequest = AdventureRouteRequest(
        userLocation = location,
        durationMinutes = durationMinutes,
        interests = CulturalInterest.entries
            .map { it.apiValue }
            .filter { it in interests }
            .ifEmpty { interests.toList() },
        pace = pace.apiValue,
        aiRequest = aiRequest.trim().takeIf { it.isNotEmpty() },
        startAt = startAt,
    )

    companion object {
        const val MIN_DURATION_MINUTES = 30
        const val MAX_DURATION_MINUTES = 720
        const val DEFAULT_DURATION_MINUTES = 180
        val DURATION_OPTIONS = listOf(60, 120, 180, 240, 360)
        val DEFAULT_INTERESTS = setOf(
            CulturalInterest.History.apiValue,
            CulturalInterest.TatarCulture.apiValue,
            CulturalInterest.Architecture.apiValue,
        )
    }
}

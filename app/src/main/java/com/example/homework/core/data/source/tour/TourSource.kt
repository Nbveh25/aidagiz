package com.example.homework.core.data.source.tour

import com.example.homework.entity.map.GeoLocation
import com.example.homework.entity.map.OsmPlace
import com.example.homework.entity.tour.TourProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TourSource {
    private var stops: List<OsmPlace> = emptyList()
    private val visitedIds = linkedSetOf<String>()
    private val progressState = MutableStateFlow(TourProgress())
    val progress: StateFlow<TourProgress> = progressState.asStateFlow()

    fun bindStops(places: List<OsmPlace>) {
        stops = places
        val validIds = places.map { it.id }.toSet()
        visitedIds.retainAll(validIds)
        val total = stops.size.coerceAtLeast(1)
        progressState.value = TourProgress(currentStep = 1, totalSteps = total)
    }

    fun bindStopIds(placeIds: List<String>) {
        bindStops(
            placeIds.map { id ->
                stops.firstOrNull { it.id == id } ?: OsmPlace(
                    id = id,
                    name = "",
                    lat = 0.0,
                    lon = 0.0,
                    category = com.example.homework.entity.map.PlaceCategory.Other,
                )
            },
        )
    }

    fun selectStop(placeId: String) {
        val index = stops.indexOfFirst { it.id == placeId }
        if (index >= 0) {
            progressState.update { it.copy(currentStep = index + 1) }
        }
    }

    fun markCurrentVisited() {
        currentStopId()?.let(visitedIds::add)
    }

    fun currentStopId(): String? =
        stops.getOrNull(progressState.value.currentStep - 1)?.id

    fun nextStopId(): String? {
        val current = progressState.value
        if (current.currentStep >= current.totalSteps) return currentStopId()
        progressState.update { it.copy(currentStep = it.currentStep + 1) }
        return currentStopId()
    }

    fun previousStopId(): String? {
        val current = progressState.value
        if (current.currentStep <= 1) return currentStopId()
        progressState.update { it.copy(currentStep = it.currentStep - 1) }
        return currentStopId()
    }

    fun visitedLocations(): List<GeoLocation> =
        stops.filter { it.id in visitedIds }.map { GeoLocation(it.lat, it.lon) }

    fun remainingLocations(): List<GeoLocation> {
        val currentIndex = (progressState.value.currentStep - 1).coerceAtLeast(0)
        return stops.drop(currentIndex)
            .filter { it.id !in visitedIds }
            .map { GeoLocation(it.lat, it.lon) }
    }

    fun currentPlace(): OsmPlace? =
        stops.getOrNull(progressState.value.currentStep - 1)
}

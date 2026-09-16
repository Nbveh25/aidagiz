package com.example.homework.core.domain.usecase.impl

import com.example.homework.core.domain.repository.AiGuideRepository
import com.example.homework.core.domain.repository.RouteRepository
import com.example.homework.core.domain.repository.TourRepository
import com.example.homework.core.domain.usecase.ContinueRouteUseCase
import com.example.homework.entity.map.GeoLocation

class ContinueRouteUseCaseImpl(
    private val tourRepository: TourRepository,
    private val routeRepository: RouteRepository,
    private val aiGuideRepository: AiGuideRepository,
) : ContinueRouteUseCase {
    override suspend fun invoke(location: GeoLocation): String? {
        tourRepository.markCurrentVisited()
        aiGuideRepository.resetForPlace()
        val remaining = tourRepository.remainingLocations()
        if (remaining.isEmpty()) {
            return tourRepository.nextStopId()
        }
        val rebuilt = routeRepository.rebuildRoute(
            userLocation = location,
            visitedPlaces = tourRepository.visitedLocations(),
            remainingPlaces = remaining,
            aiRequest = CONTINUE_REQUEST,
        )
        tourRepository.bindStops(rebuilt.places)
        return tourRepository.currentStopId() ?: rebuilt.places.firstOrNull()?.id
    }

    private companion object {
        const val CONTINUE_REQUEST = "Продолжи маршрут, оставь доступные точки"
    }
}

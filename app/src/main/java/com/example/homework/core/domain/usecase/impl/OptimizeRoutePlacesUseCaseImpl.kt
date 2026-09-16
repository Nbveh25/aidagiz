package com.example.homework.core.domain.usecase.impl

import com.example.homework.core.domain.repository.OsrmRepository
import com.example.homework.core.domain.usecase.OptimizeRoutePlacesUseCase
import com.example.homework.entity.map.GeoLocation
import com.example.homework.entity.map.RoutePlace
import com.example.homework.entity.map.TransportMode
import com.example.homework.entity.map.optimizeRoutePlaces

class OptimizeRoutePlacesUseCaseImpl(
    private val osrmRepository: OsrmRepository,
) : OptimizeRoutePlacesUseCase {
    override suspend fun invoke(
        user: GeoLocation,
        places: List<RoutePlace>,
        mode: TransportMode,
    ): List<RoutePlace> {
        if (places.size <= 1) return places
        val matrix = osrmRepository.getRouteMatrix(
            points = listOf(user) + places.map { it.location },
            mode = mode,
        )
        return optimizeRoutePlaces(places, matrix)
    }
}

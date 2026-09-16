package com.example.homework.core.domain.usecase.impl

import com.example.homework.core.domain.repository.RouteRepository
import com.example.homework.core.domain.usecase.BuildAdventureRouteUseCase
import com.example.homework.entity.map.GeoLocation
import com.example.homework.entity.tour.AdventureRoute

class BuildAdventureRouteUseCaseImpl(
    private val routeRepository: RouteRepository,
) : BuildAdventureRouteUseCase {
    override suspend fun invoke(location: GeoLocation): AdventureRoute =
        routeRepository.buildRoute(location)
}

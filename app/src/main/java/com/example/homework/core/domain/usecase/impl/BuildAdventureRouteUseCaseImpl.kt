package com.example.homework.core.domain.usecase.impl

import com.example.homework.core.domain.repository.RouteRepository
import com.example.homework.core.domain.usecase.BuildAdventureRouteUseCase
import com.example.homework.entity.tour.AdventureRoute
import com.example.homework.entity.tour.AdventureRouteRequest

class BuildAdventureRouteUseCaseImpl(
    private val routeRepository: RouteRepository,
) : BuildAdventureRouteUseCase {
    override suspend fun invoke(request: AdventureRouteRequest): AdventureRoute =
        routeRepository.buildRoute(request)
}

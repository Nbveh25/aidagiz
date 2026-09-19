package com.example.homework.core.domain.usecase

import com.example.homework.entity.tour.AdventureRoute
import com.example.homework.entity.tour.AdventureRouteRequest

interface BuildAdventureRouteUseCase {
    suspend operator fun invoke(request: AdventureRouteRequest): AdventureRoute
}

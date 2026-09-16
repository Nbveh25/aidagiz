package com.example.homework.core.domain.usecase

import com.example.homework.entity.map.GeoLocation
import com.example.homework.entity.tour.AdventureRoute

interface BuildAdventureRouteUseCase {
    suspend operator fun invoke(location: GeoLocation): AdventureRoute
}

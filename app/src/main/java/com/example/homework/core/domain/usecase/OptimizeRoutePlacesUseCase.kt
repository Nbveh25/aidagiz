package com.example.homework.core.domain.usecase

import com.example.homework.entity.map.GeoLocation
import com.example.homework.entity.map.RoutePlace
import com.example.homework.entity.map.TransportMode

interface OptimizeRoutePlacesUseCase {
    suspend operator fun invoke(
        user: GeoLocation,
        places: List<RoutePlace>,
        mode: TransportMode,
    ): List<RoutePlace>
}

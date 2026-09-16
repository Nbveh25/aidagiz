package com.example.homework.core.domain.usecase

import com.example.homework.entity.map.GeoLocation
import com.example.homework.entity.map.RouteResult
import com.example.homework.entity.map.TransportMode

interface GetOsrmRouteUseCase {
    suspend operator fun invoke(points: List<GeoLocation>, mode: TransportMode): RouteResult
}

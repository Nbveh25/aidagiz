package com.example.homework.core.domain.usecase.impl

import com.example.homework.core.domain.repository.OsrmRepository
import com.example.homework.core.domain.usecase.GetOsrmRouteUseCase
import com.example.homework.entity.map.GeoLocation
import com.example.homework.entity.map.RouteResult
import com.example.homework.entity.map.TransportMode

class GetOsrmRouteUseCaseImpl(
    private val osrmRepository: OsrmRepository,
) : GetOsrmRouteUseCase {
    override suspend fun invoke(points: List<GeoLocation>, mode: TransportMode): RouteResult =
        osrmRepository.getRoute(points, mode)
}

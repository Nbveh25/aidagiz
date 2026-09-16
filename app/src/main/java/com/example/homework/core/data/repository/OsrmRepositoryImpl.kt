package com.example.homework.core.data.repository

import com.example.homework.core.data.source.osrm.OsrmSource
import com.example.homework.core.domain.repository.OsrmRepository
import com.example.homework.entity.map.GeoLocation
import com.example.homework.entity.map.RouteMatrix
import com.example.homework.entity.map.RouteResult
import com.example.homework.entity.map.TransportMode

class OsrmRepositoryImpl(
    private val osrmSource: OsrmSource,
) : OsrmRepository {
    override suspend fun getRoute(points: List<GeoLocation>, mode: TransportMode): RouteResult =
        osrmSource.getRoute(points, mode)

    override suspend fun getRouteMatrix(points: List<GeoLocation>, mode: TransportMode): RouteMatrix =
        osrmSource.getRouteMatrix(points, mode)
}

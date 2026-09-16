package com.example.homework.core.domain.repository

import com.example.homework.entity.map.GeoLocation
import com.example.homework.entity.map.RouteMatrix
import com.example.homework.entity.map.RouteResult
import com.example.homework.entity.map.TransportMode

interface OsrmRepository {
    suspend fun getRoute(points: List<GeoLocation>, mode: TransportMode): RouteResult
    suspend fun getRouteMatrix(points: List<GeoLocation>, mode: TransportMode): RouteMatrix
}

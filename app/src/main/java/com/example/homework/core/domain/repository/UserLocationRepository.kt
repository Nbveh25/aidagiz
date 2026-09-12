package com.example.homework.core.domain.repository

import com.example.homework.entity.map.GeoLocation
import kotlinx.coroutines.flow.Flow

interface UserLocationRepository {
    fun hasPermission(): Boolean
    fun getUserLocation(): Flow<GeoLocation>
}

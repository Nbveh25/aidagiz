package com.example.homework.core.data.repository

import com.example.homework.core.data.source.userLocation.UserLocationSource
import com.example.homework.core.domain.repository.UserLocationRepository
import com.example.homework.entity.map.GeoLocation
import kotlinx.coroutines.flow.Flow

class UserLocationRepositoryImpl(
    private val userLocationSource: UserLocationSource,
) : UserLocationRepository {
    override fun hasPermission(): Boolean =
        userLocationSource.hasPermission()

    override fun getUserLocation(): Flow<GeoLocation> =
        userLocationSource.getUserLocation()
}

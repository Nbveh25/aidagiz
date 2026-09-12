package com.example.homework.core.domain.usecase.impl

import com.example.homework.core.domain.repository.UserLocationRepository
import com.example.homework.core.domain.usecase.GetUserLocationUseCase
import com.example.homework.entity.map.GeoLocation
import kotlinx.coroutines.flow.Flow

class GetUserLocationUseCaseImpl(
    private val userLocationRepository: UserLocationRepository,
) : GetUserLocationUseCase {
    override fun hasPermission(): Boolean =
        userLocationRepository.hasPermission()

    override fun invoke(): Flow<GeoLocation> =
        userLocationRepository.getUserLocation()
}

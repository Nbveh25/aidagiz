package com.example.homework.core.domain.usecase

import com.example.homework.entity.map.GeoLocation
import kotlinx.coroutines.flow.Flow

interface GetUserLocationUseCase {
    fun hasPermission(): Boolean
    operator fun invoke(): Flow<GeoLocation>
}

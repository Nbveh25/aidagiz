package com.example.homework.core.domain.usecase

import com.example.homework.entity.map.GeoLocation

interface ContinueRouteUseCase {
    suspend operator fun invoke(location: GeoLocation): String?
}

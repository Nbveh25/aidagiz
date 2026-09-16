package com.example.homework.core.domain.usecase

import com.example.homework.entity.tour.LocationRating

interface RatePlaceUseCase {
    suspend operator fun invoke(placeId: String, rating: Int, review: String? = null): LocationRating
}

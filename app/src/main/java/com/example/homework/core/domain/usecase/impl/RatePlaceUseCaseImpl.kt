package com.example.homework.core.domain.usecase.impl

import com.example.homework.core.domain.repository.LocationRatingRepository
import com.example.homework.core.domain.usecase.RatePlaceUseCase
import com.example.homework.entity.tour.LocationRating

class RatePlaceUseCaseImpl(
    private val locationRatingRepository: LocationRatingRepository,
) : RatePlaceUseCase {
    override suspend fun invoke(placeId: String, rating: Int, review: String?): LocationRating =
        locationRatingRepository.upsertRating(placeId, rating, review)
}

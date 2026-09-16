package com.example.homework.core.data.repository

import com.example.homework.core.data.source.rating.LocationRatingSource
import com.example.homework.core.domain.repository.LocationRatingRepository
import com.example.homework.entity.tour.LocationRating

class LocationRatingRepositoryImpl(
    private val locationRatingSource: LocationRatingSource,
) : LocationRatingRepository {
    override suspend fun upsertRating(placeId: String, rating: Int, review: String?): LocationRating =
        locationRatingSource.upsertRating(placeId, rating, review)
}

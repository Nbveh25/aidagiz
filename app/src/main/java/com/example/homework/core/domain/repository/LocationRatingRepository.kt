package com.example.homework.core.domain.repository

import com.example.homework.entity.tour.LocationRating

interface LocationRatingRepository {
    suspend fun upsertRating(placeId: String, rating: Int, review: String?): LocationRating
}

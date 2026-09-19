package com.example.homework.core.domain.usecase

import com.example.homework.entity.place.HistoricalPlaceDetails

interface GetHistoricalPlaceDetailsUseCase {
    fun cached(placeId: String): HistoricalPlaceDetails?

    suspend operator fun invoke(placeId: String): HistoricalPlaceDetails
}

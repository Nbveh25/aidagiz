package com.example.homework.core.domain.usecase.impl

import com.example.homework.core.domain.repository.HistoricalPlacesRepository
import com.example.homework.core.domain.usecase.GetHistoricalPlaceDetailsUseCase
import com.example.homework.entity.place.HistoricalPlaceDetails

class GetHistoricalPlaceDetailsUseCaseImpl(
    private val historicalPlacesRepository: HistoricalPlacesRepository,
) : GetHistoricalPlaceDetailsUseCase {
    override fun cached(placeId: String): HistoricalPlaceDetails? =
        historicalPlacesRepository.cachedPlaceDetails(placeId)

    override suspend fun invoke(placeId: String): HistoricalPlaceDetails =
        historicalPlacesRepository.getPlaceDetails(placeId)
}

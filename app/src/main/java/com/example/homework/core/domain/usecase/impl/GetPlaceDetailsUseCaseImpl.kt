package com.example.homework.core.domain.usecase.impl

import com.example.homework.core.domain.repository.PlaceDetailsRepository
import com.example.homework.core.domain.usecase.GetPlaceDetailsUseCase
import com.example.homework.entity.map.OsmPlace
import com.example.homework.entity.place.PlaceDetails

class GetPlaceDetailsUseCaseImpl(
    private val placeDetailsRepository: PlaceDetailsRepository,
) : GetPlaceDetailsUseCase {
    override fun invoke(place: OsmPlace): PlaceDetails =
        placeDetailsRepository.getPlaceDetails(place)

    override suspend fun loadStory(placeId: String) =
        placeDetailsRepository.loadStory(placeId)
}

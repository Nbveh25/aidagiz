package com.example.homework.core.domain.usecase

import com.example.homework.entity.map.OsmPlace
import com.example.homework.entity.place.PlaceDetails

interface GetPlaceDetailsUseCase {
    operator fun invoke(place: OsmPlace): PlaceDetails
    suspend fun loadStory(placeId: String)
}

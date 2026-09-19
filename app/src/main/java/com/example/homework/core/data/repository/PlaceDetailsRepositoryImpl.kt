package com.example.homework.core.data.repository

import com.example.homework.core.data.source.place.PlaceDetailsSource
import com.example.homework.core.domain.repository.PlaceDetailsRepository
import com.example.homework.entity.map.OsmPlace
import com.example.homework.entity.place.PlaceDetails

class PlaceDetailsRepositoryImpl(
    private val placeDetailsSource: PlaceDetailsSource,
) : PlaceDetailsRepository {
    override fun getPlaceDetails(place: OsmPlace): PlaceDetails =
        placeDetailsSource.getPlaceDetails(place)

    override suspend fun loadStory(placeId: String) = Unit
}

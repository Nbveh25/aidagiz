package com.example.homework.core.domain.repository

import com.example.homework.entity.map.OsmPlace
import com.example.homework.entity.place.PlaceDetails

interface PlaceDetailsRepository {
    fun getPlaceDetails(place: OsmPlace): PlaceDetails
    suspend fun loadStory(placeId: String)
}

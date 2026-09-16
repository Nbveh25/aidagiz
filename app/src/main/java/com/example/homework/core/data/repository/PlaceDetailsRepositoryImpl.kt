package com.example.homework.core.data.repository

import com.example.homework.core.data.source.place.PlaceDetailsSource
import com.example.homework.core.data.source.place.PlaceStorySource
import com.example.homework.core.domain.repository.PlaceDetailsRepository
import com.example.homework.entity.map.OsmPlace
import com.example.homework.entity.place.PlaceDetails

class PlaceDetailsRepositoryImpl(
    private val placeDetailsSource: PlaceDetailsSource,
    private val placeStorySource: PlaceStorySource,
) : PlaceDetailsRepository {
    override fun getPlaceDetails(place: OsmPlace): PlaceDetails {
        val base = placeDetailsSource.getPlaceDetails(place)
        val story = placeStorySource.cached(place.id) ?: return base
        val short = story.story.substringBefore('.').trim()
        return base.copy(
            name = story.name?.takeIf { it.isNotBlank() } ?: base.name,
            fullDescription = story.story,
            shortDescription = if (short.isNotBlank()) "$short." else base.shortDescription,
        )
    }

    override suspend fun loadStory(placeId: String) {
        runCatching { placeStorySource.getStory(placeId) }
    }
}

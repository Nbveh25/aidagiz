package com.example.homework.core.domain.repository

import com.example.homework.entity.map.GeoLocation
import com.example.homework.entity.map.OsmPlace
import com.example.homework.entity.tour.TourProgress
import kotlinx.coroutines.flow.Flow

interface TourRepository {
    fun observeProgress(): Flow<TourProgress>
    fun bindStops(places: List<OsmPlace>)
    fun selectStop(placeId: String)
    fun markCurrentVisited()
    fun currentStopId(): String?
    fun nextStopId(): String?
    fun previousStopId(): String?
    fun visitedLocations(): List<GeoLocation>
    fun remainingLocations(): List<GeoLocation>
}

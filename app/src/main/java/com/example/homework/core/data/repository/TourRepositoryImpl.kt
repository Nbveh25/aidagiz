package com.example.homework.core.data.repository

import com.example.homework.core.data.source.tour.TourSource
import com.example.homework.core.domain.repository.TourRepository
import com.example.homework.entity.map.GeoLocation
import com.example.homework.entity.map.OsmPlace
import com.example.homework.entity.tour.TourProgress
import kotlinx.coroutines.flow.Flow

class TourRepositoryImpl(
    private val tourSource: TourSource,
) : TourRepository {
    override fun observeProgress(): Flow<TourProgress> = tourSource.progress

    override fun bindStops(places: List<OsmPlace>) = tourSource.bindStops(places)

    override fun selectStop(placeId: String) = tourSource.selectStop(placeId)

    override fun markCurrentVisited() = tourSource.markCurrentVisited()

    override fun currentStopId(): String? = tourSource.currentStopId()

    override fun nextStopId(): String? = tourSource.nextStopId()

    override fun previousStopId(): String? = tourSource.previousStopId()

    override fun visitedLocations(): List<GeoLocation> = tourSource.visitedLocations()

    override fun remainingLocations(): List<GeoLocation> = tourSource.remainingLocations()
}

package com.example.homework.core.domain.usecase

import com.example.homework.entity.map.OsmPlace
import com.example.homework.entity.tour.TourProgress
import kotlinx.coroutines.flow.Flow

interface GetTourProgressUseCase {
    fun observe(): Flow<TourProgress>
    fun bindStops(places: List<OsmPlace>)
    fun selectStop(placeId: String)
}

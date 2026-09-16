package com.example.homework.core.domain.usecase.impl

import com.example.homework.core.domain.repository.TourRepository
import com.example.homework.core.domain.usecase.GetTourProgressUseCase
import com.example.homework.entity.map.OsmPlace
import com.example.homework.entity.tour.TourProgress
import kotlinx.coroutines.flow.Flow

class GetTourProgressUseCaseImpl(
    private val tourRepository: TourRepository,
) : GetTourProgressUseCase {
    override fun observe(): Flow<TourProgress> = tourRepository.observeProgress()

    override fun bindStops(places: List<OsmPlace>) = tourRepository.bindStops(places)

    override fun selectStop(placeId: String) = tourRepository.selectStop(placeId)
}

package com.example.homework.core.domain.usecase.impl

import com.example.homework.core.domain.repository.AiGuideRepository
import com.example.homework.core.domain.repository.TourRepository
import com.example.homework.core.domain.usecase.MarkPlaceVisitedUseCase

class MarkPlaceVisitedUseCaseImpl(
    private val tourRepository: TourRepository,
    private val aiGuideRepository: AiGuideRepository,
) : MarkPlaceVisitedUseCase {
    override fun invoke(): String? {
        tourRepository.markCurrentVisited()
        aiGuideRepository.resetForPlace()
        return tourRepository.nextStopId()
    }
}

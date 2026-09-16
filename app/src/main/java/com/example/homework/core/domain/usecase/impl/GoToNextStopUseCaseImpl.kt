package com.example.homework.core.domain.usecase.impl

import com.example.homework.core.domain.repository.AiGuideRepository
import com.example.homework.core.domain.repository.TourRepository
import com.example.homework.core.domain.usecase.GoToNextStopUseCase

class GoToNextStopUseCaseImpl(
    private val tourRepository: TourRepository,
    private val aiGuideRepository: AiGuideRepository,
) : GoToNextStopUseCase {
    override fun invoke(): String? {
        aiGuideRepository.resetForPlace()
        return tourRepository.nextStopId()
    }
}

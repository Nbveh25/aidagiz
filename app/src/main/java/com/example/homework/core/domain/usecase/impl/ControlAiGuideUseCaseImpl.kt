package com.example.homework.core.domain.usecase.impl

import com.example.homework.core.domain.repository.AiGuideRepository
import com.example.homework.core.domain.usecase.ControlAiGuideUseCase

class ControlAiGuideUseCaseImpl(
    private val aiGuideRepository: AiGuideRepository,
) : ControlAiGuideUseCase {
    override fun togglePlayback() = aiGuideRepository.togglePlayback()

    override fun seek(progress: Float) = aiGuideRepository.seek(progress)

    override fun toggleMute() = aiGuideRepository.toggleMute()

    override fun resetForPlace() = aiGuideRepository.resetForPlace()

    override fun tick() = aiGuideRepository.tick()
}

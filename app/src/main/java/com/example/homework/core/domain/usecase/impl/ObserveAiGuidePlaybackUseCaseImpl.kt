package com.example.homework.core.domain.usecase.impl

import com.example.homework.core.domain.repository.AiGuideRepository
import com.example.homework.core.domain.usecase.ObserveAiGuidePlaybackUseCase
import com.example.homework.entity.guide.AiGuidePlayback
import kotlinx.coroutines.flow.Flow

class ObserveAiGuidePlaybackUseCaseImpl(
    private val aiGuideRepository: AiGuideRepository,
) : ObserveAiGuidePlaybackUseCase {
    override fun invoke(): Flow<AiGuidePlayback> =
        aiGuideRepository.observePlayback()
}

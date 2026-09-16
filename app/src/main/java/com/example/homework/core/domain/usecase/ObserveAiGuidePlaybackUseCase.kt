package com.example.homework.core.domain.usecase

import com.example.homework.entity.guide.AiGuidePlayback
import kotlinx.coroutines.flow.Flow

interface ObserveAiGuidePlaybackUseCase {
    operator fun invoke(): Flow<AiGuidePlayback>
}

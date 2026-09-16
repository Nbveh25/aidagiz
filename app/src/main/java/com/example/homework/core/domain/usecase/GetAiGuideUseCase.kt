package com.example.homework.core.domain.usecase

import com.example.homework.entity.guide.AiGuideNarration
import com.example.homework.entity.map.OsmPlace

interface GetAiGuideUseCase {
    operator fun invoke(place: OsmPlace): AiGuideNarration
    suspend fun prepareAudio(place: OsmPlace)
}

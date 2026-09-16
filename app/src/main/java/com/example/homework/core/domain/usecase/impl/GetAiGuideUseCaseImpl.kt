package com.example.homework.core.domain.usecase.impl

import com.example.homework.core.domain.repository.AiGuideRepository
import com.example.homework.core.domain.usecase.GetAiGuideUseCase
import com.example.homework.entity.guide.AiGuideNarration
import com.example.homework.entity.map.OsmPlace

class GetAiGuideUseCaseImpl(
    private val aiGuideRepository: AiGuideRepository,
) : GetAiGuideUseCase {
    override fun invoke(place: OsmPlace): AiGuideNarration =
        aiGuideRepository.getNarration(place)

    override suspend fun prepareAudio(place: OsmPlace) {
        aiGuideRepository.prepareAudio(place)
    }
}

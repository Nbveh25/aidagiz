package com.example.homework.core.data.repository

import com.example.homework.core.data.source.guide.AiGuideSource
import com.example.homework.core.domain.repository.AiGuideRepository
import com.example.homework.core.domain.repository.VoiceReadingRepository
import com.example.homework.entity.guide.AiGuideNarration
import com.example.homework.entity.guide.AiGuidePlayback
import com.example.homework.entity.map.OsmPlace
import kotlinx.coroutines.flow.Flow

class AiGuideRepositoryImpl(
    private val aiGuideSource: AiGuideSource,
    private val voiceReadingRepository: VoiceReadingRepository,
) : AiGuideRepository {
    private var preparedPlaceId: String? = null
    private var preparedText: String? = null

    override fun getNarration(place: OsmPlace): AiGuideNarration =
        aiGuideSource.getNarration(place)

    override suspend fun prepareAudio(place: OsmPlace) {
        val narration = aiGuideSource.getNarration(place)
        if (
            preparedPlaceId == place.id &&
            preparedText == narration.text &&
            aiGuideSource.hasPreparedAudio()
        ) {
            aiGuideSource.resetForPlace()
            return
        }
        val wav = voiceReadingRepository.synthesize(narration.text)
        aiGuideSource.prepareAudio(wav)
        preparedPlaceId = place.id
        preparedText = narration.text
    }

    override fun observePlayback(): Flow<AiGuidePlayback> =
        aiGuideSource.playback

    override fun togglePlayback() = aiGuideSource.togglePlayback()

    override fun seek(progress: Float) = aiGuideSource.seek(progress)

    override fun toggleMute() = aiGuideSource.toggleMute()

    override fun resetForPlace() = aiGuideSource.resetForPlace()

    override fun tick() = aiGuideSource.tick()
}

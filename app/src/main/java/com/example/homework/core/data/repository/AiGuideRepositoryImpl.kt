package com.example.homework.core.data.repository

import com.example.homework.core.data.source.guide.AiGuideSource
import com.example.homework.core.data.source.place.PlaceStorySource
import com.example.homework.core.domain.repository.AiGuideRepository
import com.example.homework.core.domain.repository.VoiceReadingRepository
import com.example.homework.entity.guide.AiGuideNarration
import com.example.homework.entity.guide.AiGuidePlayback
import com.example.homework.entity.map.OsmPlace
import kotlinx.coroutines.flow.Flow

class AiGuideRepositoryImpl(
    private val aiGuideSource: AiGuideSource,
    private val voiceReadingRepository: VoiceReadingRepository,
    private val placeStorySource: PlaceStorySource,
) : AiGuideRepository {
    override fun getNarration(place: OsmPlace): AiGuideNarration =
        aiGuideSource.getNarration(place, placeStorySource.cached(place.id)?.story)

    override suspend fun prepareAudio(place: OsmPlace) {
        val story = runCatching { placeStorySource.getStory(place.id) }.getOrNull()
        val narration = aiGuideSource.getNarration(place, story?.story)
        runCatching {
            val wav = voiceReadingRepository.synthesize(narration.text)
            aiGuideSource.prepareAudio(wav)
        }
    }

    override fun observePlayback(): Flow<AiGuidePlayback> =
        aiGuideSource.playback

    override fun togglePlayback() = aiGuideSource.togglePlayback()

    override fun seek(progress: Float) = aiGuideSource.seek(progress)

    override fun toggleMute() = aiGuideSource.toggleMute()

    override fun resetForPlace() = aiGuideSource.resetForPlace()

    override fun tick() = aiGuideSource.tick()
}

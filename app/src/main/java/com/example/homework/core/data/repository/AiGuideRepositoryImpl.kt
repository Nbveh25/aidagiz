package com.example.homework.core.data.repository

import com.example.homework.core.data.source.guide.AiGuideSource
import com.example.homework.core.data.source.place.PlaceStorySource
import com.example.homework.core.domain.repository.AiGuideRepository
import com.example.homework.core.domain.repository.HistoricalPlacesRepository
import com.example.homework.core.domain.repository.VoiceReadingRepository
import com.example.homework.entity.guide.AiGuideNarration
import com.example.homework.entity.guide.AiGuidePlayback
import com.example.homework.entity.map.OsmPlace
import kotlinx.coroutines.flow.Flow

class AiGuideRepositoryImpl(
    private val aiGuideSource: AiGuideSource,
    private val voiceReadingRepository: VoiceReadingRepository,
    private val placeStorySource: PlaceStorySource,
    private val historicalPlacesRepository: HistoricalPlacesRepository,
) : AiGuideRepository {
    override fun getNarration(place: OsmPlace): AiGuideNarration =
        aiGuideSource.getNarration(place, cachedStory(place))

    override suspend fun prepareAudio(place: OsmPlace) {
        val story = loadStory(place)
        val narration = aiGuideSource.getNarration(place, story)
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

    private fun cachedStory(place: OsmPlace): String? {
        val story = if (place.isHistorical) {
            historicalPlacesRepository.cachedPlaceDetails(place.id)?.story
        } else {
            placeStorySource.cached(place.id)?.story
        }
        return story?.takeIf { it.isNotBlank() }
    }

    private suspend fun loadStory(place: OsmPlace): String? {
        cachedStory(place)?.let { return it }
        val story = if (place.isHistorical) {
            runCatching { historicalPlacesRepository.getPlaceDetails(place.id).story }.getOrNull()
        } else {
            runCatching { placeStorySource.getStory(place.id) }.getOrNull()?.story
        }
        return story?.takeIf { it.isNotBlank() }
    }
}

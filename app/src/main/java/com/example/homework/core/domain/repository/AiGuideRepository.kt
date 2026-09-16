package com.example.homework.core.domain.repository

import com.example.homework.entity.guide.AiGuideNarration
import com.example.homework.entity.guide.AiGuidePlayback
import com.example.homework.entity.map.OsmPlace
import kotlinx.coroutines.flow.Flow

interface AiGuideRepository {
    fun getNarration(place: OsmPlace): AiGuideNarration
    suspend fun prepareAudio(place: OsmPlace)
    fun observePlayback(): Flow<AiGuidePlayback>
    fun togglePlayback()
    fun seek(progress: Float)
    fun toggleMute()
    fun resetForPlace()
    fun tick()
}

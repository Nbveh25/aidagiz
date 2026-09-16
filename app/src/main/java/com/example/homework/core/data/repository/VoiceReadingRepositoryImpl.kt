package com.example.homework.core.data.repository

import com.example.homework.core.data.source.guide.VoiceReadingSource
import com.example.homework.core.domain.repository.VoiceReadingRepository

class VoiceReadingRepositoryImpl(
    private val voiceReadingSource: VoiceReadingSource,
) : VoiceReadingRepository {
    override suspend fun synthesize(text: String, language: String): ByteArray =
        voiceReadingSource.synthesize(text, language)
}

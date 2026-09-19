package com.example.homework.core.domain.repository

interface VoiceReadingRepository {
    suspend fun synthesize(text: String): ByteArray
}

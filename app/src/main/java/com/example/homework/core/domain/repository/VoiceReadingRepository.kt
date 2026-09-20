package com.example.homework.core.domain.repository

interface VoiceReadingRepository {
    suspend fun synthesize(text: String): ByteArray
    suspend fun synthesizeAll(text: String): ByteArray
}

package com.example.homework.core.data.source.guide

import com.example.homework.core.data.source.api.GuideApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class VoiceReadingSource(
    private val api: GuideApiClient,
) {
    suspend fun synthesize(text: String): ByteArray = withContext(Dispatchers.IO) {
        val clipped = text.trim().take(MAX_TEXT_LENGTH)
        require(clipped.isNotBlank()) { "Текст для озвучивания пуст" }
        api.postBytes(
            path = "/speech/synthesize",
            body = JSONObject().put("text", clipped),
            accept = "audio/wav",
        )
    }

    private companion object {
        const val MAX_TEXT_LENGTH = 1000
    }
}

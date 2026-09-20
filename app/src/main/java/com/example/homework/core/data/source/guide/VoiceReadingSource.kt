package com.example.homework.core.data.source.guide

import com.example.homework.core.data.source.api.GuideApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class VoiceReadingSource(
    private val api: GuideApiClient,
) {
    suspend fun synthesize(text: String): ByteArray = withContext(Dispatchers.IO) {
        synthesizeChunk(text.trim().take(MAX_TEXT_LENGTH))
    }

    suspend fun synthesizeAll(text: String): ByteArray = withContext(Dispatchers.IO) {
        val chunks = splitForSynthesis(text)
        require(chunks.isNotEmpty()) { "Текст для озвучивания пуст" }
        val parts = chunks.map { synthesizeChunk(it) }
        WavConcat.concatenate(parts)
    }

    private fun synthesizeChunk(text: String): ByteArray {
        val clipped = text.trim()
        require(clipped.isNotBlank()) { "Текст для озвучивания пуст" }
        return api.postBytes(
            path = "/speech/synthesize",
            body = JSONObject().put("text", clipped),
            accept = "audio/wav",
        )
    }

    companion object {
        const val MAX_TEXT_LENGTH = 1000
        const val MAX_CHUNKS = 4

        fun splitForSynthesis(text: String, limit: Int = MAX_TEXT_LENGTH, maxChunks: Int = MAX_CHUNKS): List<String> {
            val source = text.trim()
            if (source.isEmpty()) return emptyList()
            if (source.length <= limit) return listOf(source)
            val chunks = mutableListOf<String>()
            var rest = source
            while (rest.isNotEmpty() && chunks.size < maxChunks) {
                if (rest.length <= limit) {
                    chunks.add(rest)
                    break
                }
                val window = rest.take(limit)
                val splitAt = window.lastIndexOfAny(charArrayOf('.', '!', '?', '…', '\n'))
                    .takeIf { it >= limit / 3 }
                    ?: window.lastIndexOf(' ').takeIf { it >= limit / 3 }
                    ?: limit
                chunks.add(window.take(splitAt).trim())
                rest = rest.drop(splitAt).trim()
            }
            return chunks.filter { it.isNotBlank() }
        }
    }
}

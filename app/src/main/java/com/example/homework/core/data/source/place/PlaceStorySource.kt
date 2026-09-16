package com.example.homework.core.data.source.place

import com.example.homework.core.data.source.api.GuideApiClient
import com.example.homework.core.data.source.api.stringOrNull
import com.example.homework.entity.place.PlaceStory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class PlaceStorySource(
    private val api: GuideApiClient,
) {
    private val cache = ConcurrentHashMap<String, PlaceStory>()

    fun cached(placeId: String): PlaceStory? = cache[placeId]

    suspend fun getStory(placeId: String): PlaceStory? = withContext(Dispatchers.IO) {
        cache[placeId] ?: fetch(placeId)
    }

    private fun fetch(placeId: String): PlaceStory? {
        if (placeId.isBlank()) return null
        val json = api.getJson(listOf("places", placeId, "story"))
        val story = json.stringOrNull("story") ?: return null
        val sourcesJson = json.optJSONArray("sources")
        val sources = buildList {
            if (sourcesJson != null) {
                for (index in 0 until sourcesJson.length()) {
                    val source = sourcesJson.optString(index)
                    if (source.isNotBlank()) add(source)
                }
            }
        }
        val result = PlaceStory(
            placeId = json.stringOrNull("placeId") ?: placeId,
            name = json.stringOrNull("name"),
            story = story,
            sources = sources,
        )
        cache[placeId] = result
        return result
    }
}

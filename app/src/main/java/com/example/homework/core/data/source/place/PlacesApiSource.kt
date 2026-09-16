package com.example.homework.core.data.source.place

import com.example.homework.core.data.source.api.GuideApiClient
import com.example.homework.core.data.source.api.GuideApiConfig
import com.example.homework.core.data.source.api.coordString
import com.example.homework.core.data.source.api.stringOrNull
import com.example.homework.entity.map.GeoLocation
import com.example.homework.entity.map.OsmPlace
import com.example.homework.entity.map.PlaceCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class PlacesApiSource(
    private val api: GuideApiClient,
) {
    suspend fun getPlacesInArea(location: GeoLocation, deltaDegrees: Double = 0.04): List<OsmPlace> =
        withContext(Dispatchers.IO) {
            val json = api.getJson(
                path = "/api/places/in-area",
                query = mapOf(
                    "topLeftLat" to coordString(location.lat + deltaDegrees),
                    "topLeftLon" to coordString(location.lon - deltaDegrees),
                    "bottomRightLat" to coordString(location.lat - deltaDegrees),
                    "bottomRightLon" to coordString(location.lon + deltaDegrees),
                ),
            )
            val places = json.optJSONArray("places") ?: return@withContext emptyList()
            buildList {
                for (index in 0 until places.length()) {
                    val item = places.optJSONObject(index) ?: continue
                    add(item.toAreaPlace())
                }
            }
        }

    private fun JSONObject.toAreaPlace(): OsmPlace {
        val type = optString("type")
        val description = optString("description")
        val category = PlaceCategory.fromApiType(type).let { mapped ->
            if (mapped == PlaceCategory.Temple && "мечет" in description.lowercase()) {
                PlaceCategory.Mosque
            } else {
                mapped
            }
        }
        return OsmPlace(
            id = optString("id"),
            name = stringOrNull("name") ?: displayName(type, description),
            lat = getDouble("lat"),
            lon = getDouble("lon"),
            category = category,
            description = description,
            imageUrl = GuideApiConfig.rewriteMediaUrl(stringOrNull("imageUrl")),
            categoryIconUrl = GuideApiConfig.rewriteMediaUrl(stringOrNull("categoryIconUrl")),
        )
    }

    private fun displayName(type: String, description: String): String {
        val fromDescription = description.substringBefore('.').trim()
        if (fromDescription.isNotBlank()) return fromDescription
        return type.replaceFirstChar { char -> char.uppercase() }.ifBlank { "Место" }
    }
}

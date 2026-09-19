package com.example.homework.core.data.source.place

import com.example.homework.core.data.source.api.GuideApiClient
import com.example.homework.core.data.source.api.coordString
import com.example.homework.core.data.source.api.intOrNull
import com.example.homework.core.data.source.api.stringOrNull
import com.example.homework.entity.map.GeoLocation
import com.example.homework.entity.map.OsmPlace
import com.example.homework.entity.map.PlaceCategory
import com.example.homework.entity.map.YearRange
import com.example.homework.entity.place.HistoricalPlaceDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

class HistoricalPlacesApiSource(
    private val api: GuideApiClient,
) {
    private val detailsCache = ConcurrentHashMap<String, HistoricalPlaceDetails>()

    suspend fun getHistoricalPlaces(
        location: GeoLocation,
        radiusMeters: Int,
        years: YearRange,
    ): List<OsmPlace> = withContext(Dispatchers.IO) {
        val json = api.getJson(
            path = "/historical-places",
            query = mapOf(
                "lat" to coordString(location.lat),
                "lon" to coordString(location.lon),
                "radiusMeters" to radiusMeters.toString(),
                "yearFrom" to years.from.toString(),
                "yearTo" to years.to.toString(),
            ),
        )
        val places = json.optJSONArray("places") ?: return@withContext emptyList()
        buildList {
            for (index in 0 until places.length()) {
                val item = places.optJSONObject(index) ?: continue
                val id = item.optString("id").trim()
                if (id.isEmpty() || !item.has("lat") || !item.has("lon")) continue
                add(item.toHistoricalPlace(id))
            }
        }
    }

    fun cachedPlaceDetails(placeId: String): HistoricalPlaceDetails? = detailsCache[placeId]

    suspend fun getPlaceDetails(placeId: String): HistoricalPlaceDetails = withContext(Dispatchers.IO) {
        detailsCache[placeId] ?: fetchPlaceDetails(placeId).also { detailsCache[placeId] = it }
    }

    private fun fetchPlaceDetails(placeId: String): HistoricalPlaceDetails {
        require(WIKIDATA_ID.matches(placeId)) { "Invalid Wikidata id" }
        val json = api.getJson(listOf("historical-places", placeId))
        return HistoricalPlaceDetails(
            placeId = placeId,
            imageUrl = json.stringOrNull("imageUrl"),
            nameRu = json.stringOrNull("nameRu") ?: placeId,
            nameTt = json.stringOrNull("nameTt"),
            story = json.stringOrNull("story").orEmpty(),
            firstMentionYear = json.intOrNull("firstMentionYear"),
        )
    }

    private fun JSONObject.toHistoricalPlace(id: String): OsmPlace = OsmPlace(
        id = id,
        name = DEFAULT_NAME,
        lat = getDouble("lat"),
        lon = getDouble("lon"),
        category = PlaceCategory.Historic,
        isHistorical = true,
    )

    private companion object {
        const val DEFAULT_NAME = "Историческое место"
        val WIKIDATA_ID = Regex("^Q\\d+$")
    }
}

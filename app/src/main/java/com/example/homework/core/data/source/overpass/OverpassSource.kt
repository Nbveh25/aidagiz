package com.example.homework.core.data.source.overpass

import com.example.homework.OSM_USER_AGENT
import com.example.homework.entity.map.GeoLocation
import com.example.homework.entity.map.OsmPlace
import com.example.homework.entity.map.PlaceCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class OverpassSource(
    httpClient: OkHttpClient,
) {
    private val client = httpClient.newBuilder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun getNearbyPlaces(
        location: GeoLocation,
        radiusMeters: Int = 5000,
        limit: Int = 300,
    ): List<OsmPlace> = withContext(Dispatchers.IO) {
        val query = overpassQuery(location.lat, location.lon, radiusMeters)
        val places = parsePlaces(fetchJson(query))
        places
            .distinctBy { it.id }
            .sortedBy { it.distanceMetersTo(location) }
            .take(limit)
    }

    private fun fetchJson(query: String): String {
        val body = requestBody(query)
        var lastError: Exception? = null
        for (endpoint in endpoints) {
            try {
                return post(endpoint, body)
            } catch (error: RetryableOverpassException) {
                lastError = error
            } catch (error: IOException) {
                lastError = error
            }
        }
        throw lastError ?: IOException("Overpass недоступен")
    }

    private fun post(url: String, body: FormBody): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", OSM_USER_AGENT)
            .header("Accept", "application/json")
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            val payload = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val message = "Overpass HTTP ${response.code}: ${payload.take(180)}"
                if (response.code == 406 || response.code == 429 || response.code >= 500) {
                    throw RetryableOverpassException(message)
                }
                throw IOException(message)
            }
            return payload
        }
    }

    private fun parsePlaces(json: String): List<OsmPlace> {
        val elements = JSONObject(json).optJSONArray("elements") ?: return emptyList()
        val places = ArrayList<OsmPlace>(elements.length())
        for (i in 0 until elements.length()) {
            val element = elements.optJSONObject(i) ?: continue
            val tags = element.optJSONObject("tags") ?: continue
            val name = tags.optString("name:ru").ifBlank { tags.optString("name") }
            if (name.isBlank()) continue
            val (lat, lon) = coordinatesOf(element) ?: continue
            val osmType = element.optString("type").ifBlank { "node" }
            val osmId = element.optLong("id")
            places += OsmPlace(
                id = "$osmType-$osmId",
                name = name,
                lat = lat,
                lon = lon,
                category = PlaceCategory.fromOsmTags(
                    tourism = tags.optString("tourism"),
                    historic = tags.optString("historic"),
                    amenity = tags.optString("amenity"),
                    leisure = tags.optString("leisure"),
                    religion = tags.optString("religion"),
                ),
                openingHours = tags.optString("opening_hours").ifBlank { null },
            )
        }
        return places
    }

    private fun coordinatesOf(element: JSONObject): Pair<Double, Double>? {
        if (element.has("lat") && element.has("lon")) {
            return element.getDouble("lat") to element.getDouble("lon")
        }
        val center = element.optJSONObject("center") ?: return null
        if (!center.has("lat") || !center.has("lon")) return null
        return center.getDouble("lat") to center.getDouble("lon")
    }

    private fun requestBody(query: String) =
        FormBody.Builder().add("data", query).build()

    private fun overpassQuery(lat: Double, lon: Double, radiusMeters: Int): String = """
        [out:json][timeout:25];
        (
          nwr["name"]["tourism"~"attraction|museum|gallery|artwork|viewpoint"](around:$radiusMeters,$lat,$lon);
          nwr["name"]["historic"](around:$radiusMeters,$lat,$lon);
          nwr["name"]["amenity"~"museum|place_of_worship|theatre|cafe|restaurant|arts_centre"](around:$radiusMeters,$lat,$lon);
          nwr["name"]["leisure"~"park|garden"](around:$radiusMeters,$lat,$lon);
        );
        out center;
    """.trimIndent()

    companion object {
        private val endpoints = listOf(
            "https://overpass-api.de/api/interpreter",
            "https://overpass.private.coffee/api/interpreter",
        )
    }
}

private class RetryableOverpassException(message: String) : IOException(message)

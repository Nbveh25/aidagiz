package com.example.homework.core.data.source.place

import com.example.homework.OSM_USER_AGENT
import com.example.homework.entity.map.OsmPlace
import com.example.homework.entity.tour.kazanPlaceImageUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

class PlaceImageSource(
    httpClient: OkHttpClient,
) {
    private val client = httpClient.newBuilder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
    private val cache = ConcurrentHashMap<String, String>()

    suspend fun findImageUrl(place: OsmPlace): String? = withContext(Dispatchers.IO) {
        if (!place.imageUrl.isNullOrBlank()) return@withContext place.imageUrl
        cache[place.id]?.let { return@withContext it }
        val found = kazanPlaceImageUrl(place.name)
            ?: wikidataImage(place.id)
            ?: wikipediaImage(place.name)
        if (!found.isNullOrBlank()) cache[place.id] = found
        found
    }

    private fun wikidataImage(placeId: String): String? {
        if (!WIKIDATA_ID.matches(placeId)) return null
        val json = getJson(
            "https://www.wikidata.org/w/api.php".toHttpUrl().newBuilder()
                .addQueryParameter("action", "wbgetentities")
                .addQueryParameter("ids", placeId)
                .addQueryParameter("props", "claims")
                .addQueryParameter("format", "json")
                .build()
                .toString(),
        ) ?: return null
        val fileName = json.optJSONObject("entities")
            ?.optJSONObject(placeId)
            ?.optJSONObject("claims")
            ?.optJSONArray("P18")
            ?.optJSONObject(0)
            ?.optJSONObject("mainsnak")
            ?.optJSONObject("datavalue")
            ?.optString("value")
            ?.trim()
            .orEmpty()
        if (fileName.isEmpty()) return null
        val encoded = URLEncoder.encode(fileName.replace(' ', '_'), Charsets.UTF_8.name())
        return "https://commons.wikimedia.org/wiki/Special:FilePath/$encoded?width=1280"
    }

    private fun wikipediaImage(name: String): String? {
        val query = name.trim()
        val normalized = query.lowercase().replace('ё', 'е')
        if (query.length < 3 || normalized == "историческое место" || normalized == "место") return null
        val json = getJson(
            "https://ru.wikipedia.org/w/api.php".toHttpUrl().newBuilder()
                .addQueryParameter("action", "query")
                .addQueryParameter("generator", "search")
                .addQueryParameter("gsrsearch", "$query Казань")
                .addQueryParameter("gsrlimit", "1")
                .addQueryParameter("prop", "pageimages")
                .addQueryParameter("piprop", "thumbnail")
                .addQueryParameter("pithumbsize", "1280")
                .addQueryParameter("format", "json")
                .addQueryParameter("redirects", "1")
                .build()
                .toString(),
        ) ?: return null
        val pages = json.optJSONObject("query")?.optJSONObject("pages") ?: return null
        val keys = pages.keys()
        while (keys.hasNext()) {
            val page = pages.optJSONObject(keys.next()) ?: continue
            val url = page.optJSONObject("thumbnail")?.optString("source").orEmpty()
            if (!url.startsWith("http")) continue
            val cleaned = url.substringBefore('?')
            if (cleaned.endsWith(".svg") || cleaned.contains(".svg.")) continue
            val haystack = "${page.optString("title")} $cleaned".lowercase()
            val moscowOnly = "moscow" in haystack && "kazan" !in haystack && "казан" !in haystack
            if (moscowOnly) continue
            val aboutKazan = "казан" in haystack || "kazan" in haystack || "qazan" in haystack || "tatar" in haystack
            if (!aboutKazan) continue
            return cleaned
        }
        return null
    }

    private fun getJson(url: String): JSONObject? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", OSM_USER_AGENT)
            .header("Accept", "application/json")
            .get()
            .build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                val payload = response.body?.string().orEmpty()
                if (!response.isSuccessful) return@use null
                JSONObject(payload)
            }
        }.getOrNull()
    }

    private companion object {
        val WIKIDATA_ID = Regex("^Q\\d+$")
    }
}

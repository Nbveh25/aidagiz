package com.example.homework.core.data.source.route

import com.example.homework.core.data.source.api.GuideApiClient
import com.example.homework.core.data.source.api.GuideApiConfig
import com.example.homework.core.data.source.api.stringOrNull
import com.example.homework.core.data.source.user.AnonymousUserSource
import com.example.homework.entity.map.GeoLocation
import com.example.homework.entity.map.OsmPlace
import com.example.homework.entity.map.PlaceCategory
import com.example.homework.entity.map.clampToKazan
import com.example.homework.entity.tour.AdventureRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class RouteApiSource(
    private val api: GuideApiClient,
    private val anonymousUserSource: AnonymousUserSource,
) {
    suspend fun buildRoute(
        location: GeoLocation,
        durationMinutes: Int = 240,
        interests: List<String> = DEFAULT_INTERESTS,
        pace: String = "обычный",
        requiredPlaces: List<GeoLocation> = emptyList(),
        visitedPlaces: String? = null,
        aiRequest: String? = null,
    ): AdventureRoute = withContext(Dispatchers.IO) {
        val userId = anonymousUserSource.ensureUserId()
        val start = location.clampToKazan()
        val body = JSONObject()
            .put("durationMinutes", durationMinutes)
            .put("interests", JSONArray(interests))
            .put("pace", pace)
            .put("userLocation", start.toJson())
            .put("optimizeVisitOrder", true)
        if (requiredPlaces.isNotEmpty()) {
            body.put(
                "requiredPlaces",
                JSONArray().apply {
                    requiredPlaces.forEach { put(it.clampToKazan().toJson()) }
                },
            )
        }
        if (!visitedPlaces.isNullOrBlank()) body.put("visitedPlaces", visitedPlaces)
        if (!aiRequest.isNullOrBlank()) body.put("aiRequest", aiRequest)
        api.postJson(
            path = "/route-adventure",
            body = body,
            headers = mapOf(GuideApiConfig.USER_ID_HEADER to userId),
        ).toAdventureRoute()
    }

    suspend fun rebuildRoute(
        userLocation: GeoLocation,
        visitedPlaces: List<GeoLocation>,
        remainingPlaces: List<GeoLocation>,
        aiRequest: String,
        currentAt: String? = null,
    ): AdventureRoute = withContext(Dispatchers.IO) {
        val userId = anonymousUserSource.ensureUserId()
        val body = JSONObject()
            .put("visitedPlaces", visitedPlaces.toCoordArray())
            .put("remainingPlaces", remainingPlaces.toCoordArray())
            .put("userLocation", userLocation.clampToKazan().toJson())
            .put("aiRequest", aiRequest)
        if (!currentAt.isNullOrBlank()) body.put("currentAt", currentAt)
        api.postJson(
            path = "/route-adventure/rebuild",
            body = body,
            headers = mapOf(GuideApiConfig.USER_ID_HEADER to userId),
        ).toAdventureRoute()
    }

    private fun JSONObject.toAdventureRoute(): AdventureRoute {
        val placesJson = optJSONArray("places") ?: JSONArray()
        val places = buildList {
            for (index in 0 until placesJson.length()) {
                val item = placesJson.optJSONObject(index) ?: continue
                add(item.toRoutePlace())
            }
        }
        return AdventureRoute(
            startedAt = optString("startedAt"),
            finishedAt = optString("finishedAt"),
            totalDurationMinutes = optInt("totalDurationMinutes"),
            totalTravelDurationMinutes = optInt("totalTravelDurationMinutes"),
            totalVisitDurationMinutes = optInt("totalVisitDurationMinutes"),
            places = places,
        )
    }

    private fun JSONObject.toRoutePlace(): OsmPlace = OsmPlace(
        id = optString("id"),
        name = optString("name").ifBlank { optString("type") },
        lat = getDouble("lat"),
        lon = getDouble("lon"),
        category = PlaceCategory.fromApiType(optString("type")),
        description = optString("description"),
        address = stringOrNull("address"),
        imageUrl = GuideApiConfig.rewriteMediaUrl(stringOrNull("imageUrl")),
        categoryIconUrl = GuideApiConfig.rewriteMediaUrl(stringOrNull("categoryIconUrl")),
    )

    private fun GeoLocation.toJson() = JSONObject()
        .put("lat", lat)
        .put("lon", lon)

    private fun List<GeoLocation>.toCoordArray(): JSONArray = JSONArray().apply {
        forEach { put(it.clampToKazan().toJson()) }
    }

    companion object {
        val DEFAULT_INTERESTS = listOf(
            "история",
            "татарская культура",
            "архитектура",
            "музеи",
            "мечети",
            "парки",
        )
    }
}

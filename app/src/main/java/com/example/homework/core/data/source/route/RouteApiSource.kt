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
import com.example.homework.entity.tour.AdventureRouteRequest
import com.example.homework.entity.tour.KazanTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class RouteApiSource(
    private val api: GuideApiClient,
    private val anonymousUserSource: AnonymousUserSource,
) {
    suspend fun buildRoute(request: AdventureRouteRequest): AdventureRoute =
        withContext(Dispatchers.IO) {
            val userId = anonymousUserSource.ensureUserId()
            val start = request.userLocation.clampToKazan()
            val body = JSONObject()
                .put("durationMinutes", request.durationMinutes)
                .put("interests", JSONArray(request.interests))
                .put("pace", request.pace)
                .put("userLocation", start.toJson())
                .put("optimizeVisitOrder", request.optimizeVisitOrder)
            if (request.requiredPlaces.isNotEmpty()) {
                body.put(
                    "requiredPlaces",
                    JSONArray().apply {
                        request.requiredPlaces.forEach { put(it.clampToKazan().toJson()) }
                    },
                )
            }
            if (!request.visitedPlaces.isNullOrBlank()) {
                body.put("visitedPlaces", request.visitedPlaces)
            }
            if (!request.aiRequest.isNullOrBlank()) {
                body.put("aiRequest", request.aiRequest)
            }
            body.put("startAt", request.startAt?.takeIf { it.isNotBlank() } ?: KazanTime.nowIso())
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
            .put("currentAt", currentAt?.takeIf { it.isNotBlank() } ?: KazanTime.nowIso())
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
                val place = item.toRoutePlace() ?: continue
                add(place)
            }
        }
        return AdventureRoute(
            startedAt = optString("startedAt"),
            finishedAt = optString("finishedAt"),
            totalDurationMinutes = optInt("totalDurationMinutes"),
            totalTravelDurationMinutes = optInt("totalTravelDurationMinutes"),
            totalVisitDurationMinutes = optInt("totalVisitDurationMinutes"),
            places = places,
            summary = stringOrNull("summary"),
        )
    }

    private fun JSONObject.toRoutePlace(): OsmPlace? {
        val lat = optDouble("lat", Double.NaN)
        val lon = optDouble("lon", Double.NaN)
        if (lat.isNaN() || lon.isNaN()) return null
        return OsmPlace(
            id = optString("id"),
            name = optString("name").ifBlank { optString("type") },
            lat = lat,
            lon = lon,
            category = PlaceCategory.fromApiType(optString("type")),
            description = optString("description"),
            address = stringOrNull("address"),
            imageUrl = GuideApiConfig.rewriteMediaUrl(stringOrNull("imageUrl")),
            categoryIconUrl = GuideApiConfig.rewriteMediaUrl(stringOrNull("categoryIconUrl")),
            stopOrder = intOrNull("order"),
            arrivalAt = stringOrNull("arrivalAt"),
            departureAt = stringOrNull("departureAt"),
            travelDurationMinutes = intOrNull("travelDurationMinutes"),
            visitDurationMinutes = intOrNull("visitDurationMinutes"),
        )
    }

    private fun GeoLocation.toJson() = JSONObject()
        .put("lat", lat)
        .put("lon", lon)

    private fun List<GeoLocation>.toCoordArray(): JSONArray = JSONArray().apply {
        forEach { put(it.clampToKazan().toJson()) }
    }

    private fun JSONObject.intOrNull(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        return optInt(key)
    }
}

package com.example.homework.core.data.source.rating

import com.example.homework.core.data.source.api.GuideApiClient
import com.example.homework.core.data.source.api.GuideApiConfig
import com.example.homework.core.data.source.api.stringOrNull
import com.example.homework.core.data.source.user.AnonymousUserSource
import com.example.homework.entity.tour.LocationRating
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class LocationRatingSource(
    private val api: GuideApiClient,
    private val anonymousUserSource: AnonymousUserSource,
) {
    suspend fun upsertRating(placeId: String, rating: Int, review: String?): LocationRating =
        withContext(Dispatchers.IO) {
            val userId = anonymousUserSource.ensureUserId()
            val clampedRating = rating.coerceIn(1, 5)
            val body = JSONObject()
                .put("placeId", placeId)
                .put("rating", clampedRating)
            if (review == null) {
                body.put("review", JSONObject.NULL)
            } else {
                body.put("review", review)
            }
            val json = api.putJson(
                path = "/location-ratings",
                body = body,
                headers = mapOf(GuideApiConfig.USER_ID_HEADER to userId),
            )
            LocationRating(
                id = json.optString("id"),
                userId = json.optString("userId"),
                placeId = json.optString("placeId"),
                rating = json.optInt("rating"),
                review = json.stringOrNull("review"),
                createdAt = json.optString("createdAt"),
                updatedAt = json.optString("updatedAt"),
            )
        }
}

package com.example.homework.core.data.repository

import com.example.homework.core.data.source.api.GuideApiReachability
import com.example.homework.core.data.source.api.isGuideUnavailable
import com.example.homework.core.data.source.overpass.OverpassSource
import com.example.homework.core.data.source.place.PlacesApiSource
import com.example.homework.core.domain.repository.PlacesRepository
import com.example.homework.entity.map.GeoLocation
import com.example.homework.entity.map.OsmPlace
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

class PlacesRepositoryImpl(
    private val placesApiSource: PlacesApiSource,
    private val overpassSource: OverpassSource,
    private val guideApiReachability: GuideApiReachability,
) : PlacesRepository {
    override suspend fun getPlacesInArea(
        location: GeoLocation,
        radiusMeters: Int,
        limit: Int,
    ): List<OsmPlace> {
        if (!guideApiReachability.isDown()) {
            val apiResult = runGuideCall { placesApiSource.getPlacesInArea(location) }
            val apiPlaces = apiResult.getOrNull()
            if (!apiPlaces.isNullOrEmpty()) {
                guideApiReachability.markUp()
                return apiPlaces.take(limit)
            }
            val apiError = apiResult.exceptionOrNull()
            if (apiError != null && apiError.isGuideUnavailable()) {
                guideApiReachability.markDown()
            }
            val overpassPlaces = runCatching {
                overpassSource.getNearbyPlaces(location, radiusMeters, limit)
            }.getOrElse { overpassError ->
                throw apiError ?: overpassError
            }
            if (overpassPlaces.isNotEmpty() || apiError?.isGuideUnavailable() == true) {
                return overpassPlaces
            }
            apiError?.let { throw it }
            return emptyList()
        }

        return overpassSource.getNearbyPlaces(location, radiusMeters, limit)
    }

    private companion object {
        const val GUIDE_PLACES_TIMEOUT_MS = 8_000L
    }
}

internal suspend fun <T> runGuideCall(
    timeoutMs: Long = 8_000L,
    block: suspend () -> T,
): Result<T> = try {
    Result.success(withTimeout(timeoutMs) { block() })
} catch (error: TimeoutCancellationException) {
    Result.failure(error)
} catch (error: CancellationException) {
    throw error
} catch (error: Exception) {
    Result.failure(error)
}

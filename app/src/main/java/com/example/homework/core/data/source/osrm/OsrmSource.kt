package com.example.homework.core.data.source.osrm

import com.example.homework.OSM_USER_AGENT
import com.example.homework.entity.map.GeoLocation
import com.example.homework.entity.map.RouteMatrix
import com.example.homework.entity.map.RouteResult
import com.example.homework.entity.map.RouteStep
import com.example.homework.entity.map.TransportMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class OsrmSource(
    httpClient: OkHttpClient,
) {
    private val client = httpClient.newBuilder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun getRoute(points: List<GeoLocation>, mode: TransportMode): RouteResult =
        withContext(Dispatchers.IO) {
            require(mode.isRoutable) { "Транспорт $mode считается в Яндекс Картах" }
            require(points.size >= 2) { "Нужны минимум две точки маршрута" }
            val json = getJson(routeUrl(points, mode))
            val route = json.optJSONArray("routes")?.optJSONObject(0)
                ?: throw IOException("OSRM не вернул маршрут")
            val geometry = parseLine(route.optJSONObject("geometry"))
            if (geometry.size < 2) throw IOException("Пустая геометрия маршрута")
            RouteResult(
                geometry = geometry,
                distanceMeters = route.optDouble("distance"),
                durationSeconds = route.optDouble("duration"),
                steps = parseSteps(route.optJSONArray("legs")),
                mode = mode,
            )
        }

    suspend fun getRouteMatrix(points: List<GeoLocation>, mode: TransportMode): RouteMatrix =
        withContext(Dispatchers.IO) {
            require(mode.isRoutable) { "Для этого транспорта нет OSRM-матрицы" }
            require(points.size >= 2) { "Нужны минимум две точки" }
            val json = getJson(tableUrl(points, mode))
            RouteMatrix(
                durations = parseMatrix(json.optJSONArray("durations")),
                distances = parseMatrix(json.optJSONArray("distances")),
            )
        }

    private fun getJson(url: String): JSONObject {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", OSM_USER_AGENT)
            .header("Accept", "application/json")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            val payload = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("OSRM HTTP ${response.code}: ${payload.take(180)}")
            }
            val json = JSONObject(payload)
            val code = json.optString("code")
            if (code.isNotBlank() && !code.equals("Ok", ignoreCase = true)) {
                throw IOException(json.optString("message").ifBlank { "OSRM: $code" })
            }
            return json
        }
    }

    private fun parseLine(geometry: JSONObject?): List<GeoLocation> {
        val coords = geometry?.optJSONArray("coordinates") ?: return emptyList()
        return buildList {
            for (index in 0 until coords.length()) {
                val pair = coords.optJSONArray(index) ?: continue
                if (pair.length() < 2) continue
                add(GeoLocation(lat = pair.getDouble(1), lon = pair.getDouble(0)))
            }
        }
    }

    private fun parseSteps(legs: JSONArray?): List<RouteStep> {
        if (legs == null) return emptyList()
        val steps = ArrayList<RouteStep>()
        for (legIndex in 0 until legs.length()) {
            val leg = legs.optJSONObject(legIndex) ?: continue
            val items = leg.optJSONArray("steps") ?: continue
            for (stepIndex in 0 until items.length()) {
                val step = items.optJSONObject(stepIndex) ?: continue
                val maneuver = step.optJSONObject("maneuver") ?: JSONObject()
                val location = maneuver.optJSONArray("location")
                val lon = location?.optDouble(0) ?: continue
                val lat = location.optDouble(1)
                steps += RouteStep(
                    maneuverType = maneuver.optString("type"),
                    modifier = maneuver.optString("modifier").takeIf { it.isNotBlank() },
                    location = GeoLocation(lat = lat, lon = lon),
                    distanceMeters = step.optDouble("distance"),
                    durationSeconds = step.optDouble("duration"),
                    name = step.optString("name"),
                )
            }
        }
        return steps
    }

    private fun parseMatrix(array: JSONArray?): List<List<Double?>> {
        if (array == null) return emptyList()
        return buildList {
            for (rowIndex in 0 until array.length()) {
                val row = array.optJSONArray(rowIndex) ?: continue
                add(
                    buildList {
                        for (colIndex in 0 until row.length()) {
                            add(if (row.isNull(colIndex)) null else row.optDouble(colIndex))
                        }
                    },
                )
            }
        }
    }

    private fun routeUrl(points: List<GeoLocation>, mode: TransportMode): String =
        "${baseUrl(mode)}/route/v1/driving/${coordsPath(points)}" +
            "?overview=full&geometries=geojson&steps=true&alternatives=false"

    private fun tableUrl(points: List<GeoLocation>, mode: TransportMode): String =
        "${baseUrl(mode)}/table/v1/driving/${coordsPath(points)}?annotations=duration,distance"

    private fun coordsPath(points: List<GeoLocation>): String =
        points.joinToString(";") { "${it.lon},${it.lat}" }

    private fun baseUrl(mode: TransportMode): String = when (mode) {
        TransportMode.Walking -> "https://routing.openstreetmap.de/routed-foot"
        TransportMode.Cycling -> "https://routing.openstreetmap.de/routed-bike"
        TransportMode.Driving -> "https://routing.openstreetmap.de/routed-car"
        TransportMode.Transit -> error("transit не строится через OSRM")
    }
}

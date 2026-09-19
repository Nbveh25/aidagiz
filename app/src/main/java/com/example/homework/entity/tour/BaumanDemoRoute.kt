package com.example.homework.entity.tour

import com.example.homework.entity.map.OsmPlace
import com.example.homework.entity.map.PlaceCategory
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

fun AdventureRouteRequest.matchesBaumanDemo(): Boolean {
    val wish = aiRequest.orEmpty().lowercase().replace('ё', 'е').replace(Regex("\\s+"), " ").trim()
    val selectedInterests = interests.map { it.lowercase().trim() }.toSet()
    return durationMinutes == 60 &&
        pace.equals(WalkPace.Fast.apiValue, ignoreCase = true) &&
        selectedInterests == setOf(
            CulturalInterest.Mosques.apiValue,
            CulturalInterest.Architecture.apiValue,
        ) &&
        "баумана" in wish
}

fun baumanDemoRoute(startAt: String? = null): AdventureRoute {
    val start = parseOrNow(startAt)
    val stops = demoStops()
    var cursor = start
    val places = stops.mapIndexed { index, stop ->
        cursor = cursor.plusMinutes(stop.travelDurationMinutes.toLong())
        val arrival = cursor
        cursor = cursor.plusMinutes(stop.visitDurationMinutes.toLong())
        stop.place.copy(
            stopOrder = index + 1,
            travelDurationMinutes = stop.travelDurationMinutes,
            visitDurationMinutes = stop.visitDurationMinutes,
            arrivalAt = arrival.format(ISO),
            departureAt = cursor.format(ISO),
        )
    }
    return AdventureRoute(
        startedAt = start.format(ISO),
        finishedAt = cursor.format(ISO),
        totalDurationMinutes = 60,
        totalTravelDurationMinutes = stops.sumOf { it.travelDurationMinutes },
        totalVisitDurationMinutes = stops.sumOf { it.visitDurationMinutes },
        places = places,
    )
}

private data class DemoStop(
    val place: OsmPlace,
    val travelDurationMinutes: Int,
    val visitDurationMinutes: Int,
)

private fun demoStops(): List<DemoStop> = listOf(
    DemoStop(
        place = OsmPlace(
            id = "demo-pyramid",
            name = "КРК Пирамида",
            lat = 55.794836,
            lon = 49.103090,
            category = PlaceCategory.Historic,
            description = "Культурно-развлекательный комплекс у начала прогулки.",
            address = "ул. Московская, 3",
        ),
        travelDurationMinutes = 8,
        visitDurationMinutes = 14,
    ),
    DemoStop(
        place = OsmPlace(
            id = "demo-ivanovsky",
            name = "Ивановский монастырь",
            lat = 55.794444,
            lon = 49.110278,
            category = PlaceCategory.Temple,
            description = "Иоанно-Предтеченский монастырь у стен Кремля.",
            address = "ул. Батурина, 7",
        ),
        travelDurationMinutes = 8,
        visitDurationMinutes = 14,
    ),
    DemoStop(
        place = OsmPlace(
            id = "demo-bauman",
            name = "улица Баумана",
            lat = 55.7896,
            lon = 49.1180,
            category = PlaceCategory.Attraction,
            description = "Главная пешеходная улица Казани.",
            address = "ул. Баумана",
        ),
        travelDurationMinutes = 10,
        visitDurationMinutes = 14,
    ),
)

private fun parseOrNow(startAt: String?): ZonedDateTime {
    if (startAt.isNullOrBlank()) return ZonedDateTime.now(KAZAN_ZONE)
    return runCatching { ZonedDateTime.parse(startAt) }.getOrElse {
        ZonedDateTime.now(KAZAN_ZONE)
    }
}

private val KAZAN_ZONE: ZoneId = ZoneId.of("Europe/Moscow")
private val ISO: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")

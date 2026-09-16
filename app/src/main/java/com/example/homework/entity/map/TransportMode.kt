package com.example.homework.entity.map

enum class TransportMode(val label: String) {
    Walking("Пешком"),
    Cycling("Вело"),
    Driving("Авто"),
    Transit("Транспорт"),
    ;

    val isRoutable: Boolean get() = this != Transit
}

fun buildYandexMapsRouteUrl(
    points: List<GeoLocation>,
    mode: TransportMode,
): String {
    if (points.isEmpty() || points.size > 20) return YANDEX_MAPS_HOME
    val valid = points.all { point ->
        point.lat.isFinite() &&
            point.lon.isFinite() &&
            point.lat in -90.0..90.0 &&
            point.lon in -180.0..180.0
    }
    if (!valid) return YANDEX_MAPS_HOME
    val rtext = points.joinToString("~") { "${it.lat},${it.lon}" }
    val rtt = if (mode == TransportMode.Walking) "&rtt=pd" else ""
    return "https://yandex.ru/maps/?rtext=$rtext$rtt"
}

private const val YANDEX_MAPS_HOME = "https://yandex.ru/maps"

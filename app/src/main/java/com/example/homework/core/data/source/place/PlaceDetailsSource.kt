package com.example.homework.core.data.source.place

import com.example.homework.R
import com.example.homework.entity.map.OsmPlace
import com.example.homework.entity.map.PlaceCategory
import com.example.homework.entity.place.PlaceDetails

class PlaceDetailsSource {
    fun getPlaceDetails(place: OsmPlace): PlaceDetails = PlaceDetails(
        placeId = place.id,
        name = place.name,
        shortDescription = shortDescription(place),
        fullDescription = fullDescription(place),
        address = place.address ?: if (place.isHistorical) "" else address(place.category),
        tags = tags(place.category),
        photoRes = photoRes(place),
        imageUrl = place.imageUrl,
        openingHours = place.openingHours,
        category = place.category,
    )

    private fun photoRes(place: OsmPlace): Int = when (place.id) {
        "demo-pyramid" -> R.drawable.photo_kremlin
        "demo-ivanovsky" -> R.drawable.photo_kul_sharif
        "demo-bauman" -> R.drawable.photo_bauman
        else -> when (place.category) {
            PlaceCategory.Mosque, PlaceCategory.Temple -> R.drawable.photo_kul_sharif
            PlaceCategory.Park, PlaceCategory.Garden -> R.drawable.photo_sloboda
            PlaceCategory.Cafe, PlaceCategory.Restaurant -> R.drawable.photo_bauman
            PlaceCategory.Historic, PlaceCategory.Attraction, PlaceCategory.Museum,
            PlaceCategory.Gallery, PlaceCategory.Viewpoint,
            -> R.drawable.photo_kremlin
            else -> when (place.id.hashCode().and(3)) {
                0 -> R.drawable.photo_kremlin
                1 -> R.drawable.photo_kul_sharif
                2 -> R.drawable.photo_bauman
                else -> R.drawable.photo_sloboda
            }
        }
    }

    private fun address(category: PlaceCategory): String = when (category) {
        PlaceCategory.Mosque, PlaceCategory.Historic, PlaceCategory.Museum, PlaceCategory.Gallery ->
            "Казанский Кремль"
        PlaceCategory.Cafe, PlaceCategory.Restaurant, PlaceCategory.Theatre, PlaceCategory.ArtsCentre ->
            "ул. Баумана"
        PlaceCategory.Park, PlaceCategory.Garden -> "оз. Кабан"
        else -> "центр Казани"
    }

    private fun tags(category: PlaceCategory): List<String> = when (category) {
        PlaceCategory.Mosque -> listOf("История", "Архитектура", "Татарская культура")
        PlaceCategory.Temple -> listOf("История", "Архитектура")
        PlaceCategory.Museum, PlaceCategory.Gallery -> listOf("История", "Музеи")
        PlaceCategory.Theatre -> listOf("Театры", "Архитектура")
        PlaceCategory.Cafe -> listOf("Кафе", "Еда")
        PlaceCategory.Restaurant -> listOf("Рестораны", "Татарская культура")
        PlaceCategory.Park, PlaceCategory.Garden -> listOf("Парки", "Прогулка")
        PlaceCategory.Historic -> listOf("История", "Архитектура")
        PlaceCategory.Attraction, PlaceCategory.Artwork, PlaceCategory.ArtsCentre ->
            listOf("Достопримечательности", "Фото")
        PlaceCategory.Viewpoint -> listOf("Смотровая", "Фото")
        PlaceCategory.Other -> listOf("Маршрут")
    }

    private fun shortDescription(place: OsmPlace): String {
        val fromApi = place.description.substringBefore('.').trim()
        if (fromApi.isNotBlank()) {
            return if (place.description.contains('.')) "$fromApi." else fromApi
        }
        return when (place.category) {
            PlaceCategory.Mosque -> "Историческая мечеть Казани. Архитектура и тихая атмосфера двора."
            PlaceCategory.Temple -> "Храм с богатой историей. Стоит зайти и осмотреть интерьер."
            PlaceCategory.Museum -> "Музей о городе и его культуре. Короткий визит займёт около часа."
            PlaceCategory.Theatre -> "Театр с красивым фасадом. Удобная точка на прогулке по центру."
            PlaceCategory.Cafe -> "Уютное кафе рядом. Хорошее место передохнуть и перекусить."
            PlaceCategory.Restaurant -> "Ресторан с локальной кухней. Можно запланировать обед в маршруте."
            PlaceCategory.Park, PlaceCategory.Garden ->
                "Зелёная зона для короткой прогулки и паузы между точками."
            PlaceCategory.Historic -> "Историческое место Казани. Короткий рассказ о прошлом города."
            PlaceCategory.Attraction, PlaceCategory.Artwork, PlaceCategory.ArtsCentre ->
                "Популярная точка маршрута. Несколько минут на осмотр и фото."
            PlaceCategory.Gallery -> "Галерея с работами, которые лучше смотреть без спешки."
            PlaceCategory.Viewpoint -> "Смотровая точка. Стоит остановиться ради вида на город."
            PlaceCategory.Other -> "Интересная точка поблизости. Можно добавить в маршрут."
        }
    }

    private fun fullDescription(place: OsmPlace): String {
        if (place.description.isNotBlank()) return place.description
        val name = place.name.ifBlank { "Это место" }
        return "$name — точка на прогулке по Казани. ${shortDescription(place)} " +
            "Обойдите здание или двор кругом: с разных сторон меняются детали фасада и шум улицы. " +
            "AI-гид коротко рассказывает, зачем сюда заходить и на что смотреть, прежде чем идти дальше."
    }
}

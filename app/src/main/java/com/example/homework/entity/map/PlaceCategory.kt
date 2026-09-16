package com.example.homework.entity.map

enum class PlaceCategory(
    val label: String,
    val emoji: String,
    val markerLabel: String,
    val markerColor: Long,
    val defaultVisitMinutes: Int,
) {
    Mosque("Мечеть", "🕌", "W", 0xFF9A6A16, 15),
    Temple("Храм", "⛪", "W", 0xFF9A6A16, 15),
    Museum("Музей", "🏛", "M", 0xFF315F72, 45),
    Gallery("Галерея", "🖼", "A", 0xFF315F72, 45),
    ArtsCentre("Арт-центр", "🎨", "A", 0xFF315F72, 15),
    Artwork("Искусство", "🗿", "A", 0xFF315F72, 15),
    Theatre("Театр", "🎭", "T", 0xFF695184, 40),
    Cafe("Кафе", "🥟", "C", 0xFFB45309, 20),
    Restaurant("Ресторан", "🍽", "R", 0xFFB45309, 45),
    Park("Парк", "🌳", "P", 0xFF3F7D41, 20),
    Garden("Сад", "🌿", "P", 0xFF3F7D41, 20),
    Historic("История", "🏰", "H", 0xFF8F2F3F, 25),
    Attraction("Место", "📍", "!", 0xFF8F2F3F, 20),
    Viewpoint("Смотровая", "🔭", "V", 0xFF176B4D, 10),
    Other("Место", "📍", "•", 0xFF176B4D, 15),
    ;

    companion object {
        fun fromApiType(type: String): PlaceCategory {
            val normalized = type.lowercase().trim()
            return when {
                "мечет" in normalized -> Mosque
                "храм" in normalized || "церков" in normalized -> Temple
                "religion" in normalized -> Temple
                "галер" in normalized || normalized == "gallery" -> Gallery
                normalized == "art" || "artwork" in normalized -> Artwork
                "арт" in normalized -> ArtsCentre
                "музей" in normalized || normalized == "museum" -> Museum
                "театр" in normalized || normalized == "theatre" -> Theatre
                "кафе" in normalized || normalized == "cafe" -> Cafe
                "ресторан" in normalized || "food" in normalized || "tatar_food" in normalized -> Restaurant
                "сад" in normalized || normalized == "garden" -> Garden
                "парк" in normalized || "walks_parks" in normalized -> Park
                "смотр" in normalized || normalized == "viewpoint" -> Viewpoint
                "литерат" in normalized || normalized == "literature" -> Historic
                "истори" in normalized || normalized == "history" -> Historic
                "архитектур" in normalized -> Historic
                "необычн" in normalized -> Attraction
                else -> Other
            }
        }

        fun fromOsmTags(
            tourism: String,
            historic: String,
            amenity: String,
            leisure: String,
            religion: String,
        ): PlaceCategory = when {
            tourism == "museum" || amenity == "museum" -> Museum
            tourism == "gallery" -> Gallery
            amenity == "arts_centre" -> ArtsCentre
            tourism == "artwork" -> Artwork
            tourism == "viewpoint" -> Viewpoint
            tourism == "attraction" -> Attraction
            historic.isNotBlank() -> Historic
            amenity == "place_of_worship" && religion == "muslim" -> Mosque
            amenity == "place_of_worship" -> Temple
            amenity == "theatre" -> Theatre
            amenity == "cafe" -> Cafe
            amenity == "restaurant" -> Restaurant
            leisure == "garden" -> Garden
            leisure == "park" -> Park
            tourism.isNotBlank() -> Attraction
            else -> Other
        }
    }
}

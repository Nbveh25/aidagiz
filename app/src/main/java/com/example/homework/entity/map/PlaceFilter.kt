package com.example.homework.entity.map

enum class PlaceFilter(val label: String) {
    All("Все"),
    History("История"),
    Culture("Культура"),
    Food("Еда"),
    Museums("Музеи"),
    Nature("Природа"),
    Viewpoints("Виды"),
    ;

    fun matches(category: PlaceCategory): Boolean = when (this) {
        All -> true
        History -> category == PlaceCategory.Historic || category == PlaceCategory.Attraction
        Culture -> category == PlaceCategory.Mosque ||
            category == PlaceCategory.Temple ||
            category == PlaceCategory.Theatre ||
            category == PlaceCategory.ArtsCentre ||
            category == PlaceCategory.Artwork
        Food -> category == PlaceCategory.Cafe || category == PlaceCategory.Restaurant
        Museums -> category == PlaceCategory.Museum || category == PlaceCategory.Gallery
        Nature -> category == PlaceCategory.Park || category == PlaceCategory.Garden
        Viewpoints -> category == PlaceCategory.Viewpoint
    }
}

fun filterPlaces(places: List<OsmPlace>, filter: PlaceFilter): List<OsmPlace> =
    if (filter == PlaceFilter.All) places else places.filter { filter.matches(it.category) }

fun placeFilterCount(places: List<OsmPlace>, filter: PlaceFilter): Int =
    filterPlaces(places, filter).size

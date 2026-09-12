package com.example.homework.ui.feature.map.state

import com.example.homework.entity.map.GeoLocation
import com.example.homework.entity.map.KazanCenter
import com.example.homework.entity.map.OsmPlace

data class LiveMapUiState(
    val user: GeoLocation? = null,
    val places: List<OsmPlace> = emptyList(),
    val selectedPlaceId: Long? = null,
    val isLoadingPlaces: Boolean = false,
    val errorMessage: String? = null,
    val permissionGranted: Boolean = false,
    val recenterToken: Int = 0,
) {
    val selectedPlace: OsmPlace?
        get() = places.firstOrNull { it.id == selectedPlaceId }

    val mapCenter: GeoLocation
        get() = user ?: KazanCenter
}

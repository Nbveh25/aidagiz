package com.example.homework.ui.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homework.core.domain.usecase.GetNearbyPlacesUseCase
import com.example.homework.core.domain.usecase.GetUserLocationUseCase
import com.example.homework.entity.map.GeoLocation
import com.example.homework.entity.map.distanceMeters
import com.example.homework.ui.feature.map.state.LiveMapUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LiveMapViewModel(
    private val getNearbyPlaces: GetNearbyPlacesUseCase,
    private val getUserLocation: GetUserLocationUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(
        LiveMapUiState(permissionGranted = getUserLocation.hasPermission()),
    )
    val state: StateFlow<LiveMapUiState> = _state.asStateFlow()

    private var locationJob: Job? = null
    private var placesJob: Job? = null
    private var lastQueryLocation: GeoLocation? = null
    private var didCenterOnUser = false

    init {
        refreshPlaces(_state.value.mapCenter)
        if (_state.value.permissionGranted) {
            startLocationUpdates()
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _state.update { it.copy(permissionGranted = granted) }
        if (granted) {
            startLocationUpdates()
        }
    }

    fun recenter() {
        _state.update { it.copy(recenterToken = it.recenterToken + 1) }
    }

    fun selectPlace(placeId: Long?) {
        _state.update { it.copy(selectedPlaceId = placeId) }
    }

    fun retryPlaces() {
        refreshPlaces(_state.value.mapCenter, force = true)
    }

    private fun startLocationUpdates() {
        if (locationJob?.isActive == true) return
        locationJob = viewModelScope.launch {
            getUserLocation().collect { location ->
                val shouldCenter = !didCenterOnUser
                if (shouldCenter) {
                    didCenterOnUser = true
                }
                _state.update {
                    it.copy(
                        user = location,
                        permissionGranted = true,
                        recenterToken = if (shouldCenter) it.recenterToken + 1 else it.recenterToken,
                    )
                }
                val last = lastQueryLocation
                if (last == null || distanceMeters(last.lat, last.lon, location.lat, location.lon) > 180) {
                    refreshPlaces(location)
                }
            }
        }
    }

    private fun refreshPlaces(location: GeoLocation, force: Boolean = false) {
        if (!force && lastQueryLocation == location && placesJob?.isActive == true) return
        lastQueryLocation = location
        placesJob?.cancel()
        placesJob = viewModelScope.launch {
            _state.update { it.copy(isLoadingPlaces = true, errorMessage = null) }
            try {
                val places = getNearbyPlaces(location)
                _state.update {
                    val selectedStillVisible = places.any { place -> place.id == it.selectedPlaceId }
                    it.copy(
                        places = places,
                        selectedPlaceId = if (selectedStillVisible) it.selectedPlaceId else null,
                        isLoadingPlaces = false,
                        errorMessage = null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoadingPlaces = false,
                        errorMessage = e.message ?: "Не удалось загрузить места",
                    )
                }
            }
        }
    }
}

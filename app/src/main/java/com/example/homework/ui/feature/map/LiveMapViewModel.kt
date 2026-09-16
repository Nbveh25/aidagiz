package com.example.homework.ui.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homework.core.domain.navigation.WalkingNavigation
import com.example.homework.core.domain.usecase.ControlAiGuideUseCase
import com.example.homework.core.domain.usecase.EnsureAnonymousUserUseCase
import com.example.homework.core.domain.usecase.GetAiGuideUseCase
import com.example.homework.core.domain.usecase.GetNearbyPlacesUseCase
import com.example.homework.core.domain.usecase.GetOsrmRouteUseCase
import com.example.homework.core.domain.usecase.GetPlaceDetailsUseCase
import com.example.homework.core.domain.usecase.GetTourProgressUseCase
import com.example.homework.core.domain.usecase.GetUserLocationUseCase
import com.example.homework.core.domain.usecase.GoToNextStopUseCase
import com.example.homework.core.domain.usecase.GoToPreviousStopUseCase
import com.example.homework.core.domain.usecase.MarkPlaceVisitedUseCase
import com.example.homework.core.domain.usecase.ObserveAiGuidePlaybackUseCase
import com.example.homework.core.domain.usecase.OptimizeRoutePlacesUseCase
import com.example.homework.entity.map.GeoLocation
import com.example.homework.entity.map.KazanCenter
import com.example.homework.entity.map.NavigationStatus
import com.example.homework.entity.map.OsmPlace
import com.example.homework.entity.map.PlaceFilter
import com.example.homework.entity.map.TransportMode
import com.example.homework.entity.map.addRoutePlace
import com.example.homework.entity.map.removeRoutePlace
import com.example.homework.entity.map.updateRoutePlaceVisitDuration
import com.example.homework.ui.feature.map.state.LiveMapUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LiveMapViewModel(
    private val getNearbyPlaces: GetNearbyPlacesUseCase,
    private val getUserLocation: GetUserLocationUseCase,
    private val getPlaceDetails: GetPlaceDetailsUseCase,
    private val getAiGuide: GetAiGuideUseCase,
    private val observeAiGuidePlayback: ObserveAiGuidePlaybackUseCase,
    private val controlAiGuide: ControlAiGuideUseCase,
    private val getTourProgress: GetTourProgressUseCase,
    private val goToNextStopUseCase: GoToNextStopUseCase,
    private val goToPreviousStopUseCase: GoToPreviousStopUseCase,
    private val markPlaceVisitedUseCase: MarkPlaceVisitedUseCase,
    private val ensureAnonymousUser: EnsureAnonymousUserUseCase,
    private val getOsrmRoute: GetOsrmRouteUseCase,
    private val optimizeRoutePlaces: OptimizeRoutePlacesUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(
        LiveMapUiState(permissionGranted = getUserLocation.hasPermission()),
    )
    val state: StateFlow<LiveMapUiState> = _state.asStateFlow()

    private val navigator = WalkingNavigation()
    private var locationJob: Job? = null
    private var placesJob: Job? = null
    private var routeJob: Job? = null
    private var didCenterOnUser = false
    private var didReloadAroundUser = false

    init {
        viewModelScope.launch {
            observeAiGuidePlayback().collect { playback ->
                _state.update { it.copy(guidePlayback = playback) }
            }
        }
        viewModelScope.launch {
            getTourProgress.observe().collect { progress ->
                _state.update { it.copy(tourProgress = progress) }
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(PLAYBACK_TICK_MS)
                if (_state.value.guidePlayback.isPlaying) {
                    controlAiGuide.tick()
                }
            }
        }
        viewModelScope.launch {
            runCatching { ensureAnonymousUser() }
            refreshPlaces(KazanCenter)
        }
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
        navigator.enableFollow()
        publishNavigation()
        _state.update { it.copy(recenterToken = it.recenterToken + 1) }
    }

    fun onUserMapInteraction() {
        if (navigator.state.followUser) {
            navigator.onUserDrag()
            publishNavigation()
        }
    }

    fun selectPlace(placeId: String?) {
        openPlace(placeId, openGuide = false)
    }

    fun setPlaceFilter(filter: PlaceFilter) {
        _state.update { it.copy(placeFilter = filter) }
    }

    fun toggleRoutePanel() {
        _state.update { it.copy(routePanelExpanded = !it.routePanelExpanded) }
    }

    fun toggleSelectedInRoute() {
        val place = _state.value.selectedPlace ?: return
        if (_state.value.selectedInRoute) {
            removeFromRoute(place.id)
        } else {
            addToRoute(place)
        }
    }

    fun removeFromRoute(placeId: String) {
        updateRoutePlaces(removeRoutePlace(_state.value.routePlaces, placeId), dirty = true)
    }

    fun changeVisitDuration(placeId: String, deltaMinutes: Int) {
        val current = _state.value.routePlaces.firstOrNull { it.id == placeId } ?: return
        updateRoutePlaces(
            places = updateRoutePlaceVisitDuration(
                _state.value.routePlaces,
                placeId,
                current.visitDurationMinutes + deltaMinutes,
            ),
            dirty = false,
        )
    }

    fun setTransportMode(mode: TransportMode) {
        val changed = mode != _state.value.transportMode
        _state.update { it.copy(transportMode = mode) }
        if (changed && _state.value.route != null) {
            _state.update { it.copy(routeDirty = true) }
        }
        if (mode == TransportMode.Transit) {
            _state.update { it.copy(route = null, routeDirty = false) }
        }
    }

    fun buildRoute() = requestOsrmRoute(fromNavigation = false)

    fun optimizeAndBuild() {
        val user = _state.value.user ?: _state.value.mapCenter
        val mode = _state.value.transportMode
        if (!mode.isRoutable || _state.value.routePlaces.size < 2) return
        viewModelScope.launch {
            _state.update { it.copy(isBuildingRoute = true, errorMessage = null) }
            try {
                val optimized = optimizeRoutePlaces(user, _state.value.routePlaces, mode)
                updateRoutePlaces(optimized, dirty = true)
                requestOsrmRoute(fromNavigation = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isBuildingRoute = false,
                        errorMessage = e.message ?: "Не удалось оптимизировать маршрут",
                    )
                }
            }
        }
    }

    fun startNavigation() {
        if (_state.value.transportMode != TransportMode.Walking) return
        if (_state.value.routePlaces.isEmpty()) return
        val effect = navigator.start(_state.value.routePlaces, _state.value.user)
        publishNavigation()
        _state.update { it.copy(routePanelExpanded = false, selectedPlaceId = null, isGuideOpen = false) }
        if (effect.rebuildRoute) requestOsrmRoute(fromNavigation = true)
    }

    fun pauseOrResumeNavigation() {
        when (navigator.state.status) {
            NavigationStatus.Paused -> navigator.resume()
            else -> navigator.pause()
        }
        publishNavigation()
    }

    fun retryNavigation() {
        val effect = navigator.retry(_state.value.user)
        publishNavigation()
        if (effect.rebuildRoute) requestOsrmRoute(fromNavigation = true)
    }

    fun nextNavigationPlace() {
        val effect = navigator.goToNextPlace(_state.value.user)
        publishNavigation()
        if (effect.rebuildRoute) requestOsrmRoute(fromNavigation = true)
    }

    fun stopNavigation() {
        navigator.stop()
        publishNavigation()
    }

    fun openGuide() {
        val place = _state.value.selectedPlace ?: return
        _state.update { it.copy(isGuideOpen = true) }
        viewModelScope.launch {
            runCatching { getAiGuide.prepareAudio(place) }
            if (!_state.value.guidePlayback.isPlaying) {
                controlAiGuide.togglePlayback()
            }
        }
    }

    fun closeGuide() {
        _state.update { it.copy(isGuideOpen = false) }
    }

    fun toggleGuidePlayback() = controlAiGuide.togglePlayback()

    fun seekGuide(progress: Float) = controlAiGuide.seek(progress)

    fun toggleGuideSound() = controlAiGuide.toggleMute()

    fun goToNextStop() = moveToStop(goToNextStopUseCase(), keepGuide = _state.value.isGuideOpen)

    fun goToPreviousStop() = moveToStop(goToPreviousStopUseCase(), keepGuide = true)

    fun markPlaceVisited() = moveToStop(markPlaceVisitedUseCase(), keepGuide = false)

    fun retryPlaces() {
        val location = if (didReloadAroundUser) _state.value.mapCenter else KazanCenter
        refreshPlaces(location, force = true)
    }

    private fun addToRoute(place: OsmPlace) {
        updateRoutePlaces(addRoutePlace(_state.value.routePlaces, place), dirty = true)
        _state.update { it.copy(routePanelExpanded = true) }
    }

    private fun updateRoutePlaces(
        places: List<com.example.homework.entity.map.RoutePlace>,
        dirty: Boolean,
    ) {
        val cleared = places.isEmpty()
        getTourProgress.bindStops(places.map { it.place })
        _state.update {
            it.copy(
                routePlaces = places,
                route = if (cleared) null else it.route,
                routeDirty = if (cleared) false else dirty && it.route != null,
            )
        }
    }

    private fun requestOsrmRoute(fromNavigation: Boolean) {
        val mode = _state.value.transportMode
        if (!mode.isRoutable) return
        val points = buildRoutePoints(fromNavigation)
        if (points.size < 2) {
            _state.update { it.copy(route = null, routeDirty = false, isBuildingRoute = false) }
            return
        }
        if (routeJob?.isActive == true) return
        routeJob = viewModelScope.launch {
            _state.update { it.copy(isBuildingRoute = true, errorMessage = null) }
            try {
                val result = getOsrmRoute(points, mode)
                _state.update {
                    it.copy(
                        route = result,
                        routeDirty = false,
                        isBuildingRoute = false,
                        fitRouteToken = it.fitRouteToken + 1,
                        errorMessage = null,
                    )
                }
                if (fromNavigation) {
                    navigator.onRouteBuilt(result)
                    publishNavigation()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val message = e.message ?: "Не удалось построить маршрут"
                _state.update { it.copy(isBuildingRoute = false, errorMessage = message) }
                if (fromNavigation) {
                    navigator.onRouteFailed(message)
                    publishNavigation()
                }
            }
        }
    }

    private fun buildRoutePoints(fromNavigation: Boolean): List<GeoLocation> {
        val start = _state.value.user ?: _state.value.mapCenter
        val stops = if (fromNavigation) navigator.remainingPlaces() else _state.value.routePlaces
        return listOf(start) + stops.map { it.location }
    }

    private fun publishNavigation() {
        _state.update { it.copy(navigation = navigator.state) }
        if (navigator.state.isActive) {
            _state.update { it.copy(route = navigator.state.route ?: it.route) }
        }
    }

    private fun moveToStop(nextId: String?, keepGuide: Boolean) {
        val currentId = _state.value.selectedPlaceId
        if (nextId == null || nextId == currentId) {
            _state.update { it.copy(isGuideOpen = false) }
            if (nextId == null) {
                openPlace(null, openGuide = false)
            }
            return
        }
        openPlace(nextId, openGuide = keepGuide)
        if (keepGuide) {
            _state.value.selectedPlace?.let { place ->
                viewModelScope.launch {
                    runCatching { getAiGuide.prepareAudio(place) }
                    if (!_state.value.guidePlayback.isPlaying) {
                        controlAiGuide.togglePlayback()
                    }
                }
            }
        }
    }

    private fun openPlace(placeId: String?, openGuide: Boolean) {
        if (placeId == null) {
            _state.update {
                it.copy(
                    selectedPlaceId = null,
                    placeDetails = null,
                    guideNarration = null,
                    isGuideOpen = false,
                )
            }
            return
        }
        val place = _state.value.places.firstOrNull { it.id == placeId }
            ?: _state.value.routePlaces.firstOrNull { it.id == placeId }?.place
            ?: return
        getTourProgress.selectStop(placeId)
        controlAiGuide.resetForPlace()
        _state.update {
            it.copy(
                selectedPlaceId = placeId,
                placeDetails = getPlaceDetails(place),
                guideNarration = getAiGuide(place),
                isGuideOpen = openGuide,
            )
        }
        viewModelScope.launch {
            getPlaceDetails.loadStory(place.id)
            if (_state.value.selectedPlaceId != place.id) return@launch
            _state.update {
                it.copy(
                    placeDetails = getPlaceDetails(place),
                    guideNarration = getAiGuide(place),
                )
            }
        }
    }

    private fun startLocationUpdates() {
        if (locationJob?.isActive == true) return
        locationJob = viewModelScope.launch {
            getUserLocation().collect { location ->
                val shouldCenter = !didCenterOnUser
                if (shouldCenter) didCenterOnUser = true
                _state.update {
                    it.copy(
                        user = location,
                        permissionGranted = true,
                        recenterToken = if (shouldCenter) it.recenterToken + 1 else it.recenterToken,
                    )
                }
                if (!didReloadAroundUser) {
                    didReloadAroundUser = true
                    refreshPlaces(location)
                }
                if (navigator.state.isActive) {
                    val effect = navigator.onLocation(location, System.currentTimeMillis())
                    publishNavigation()
                    if (effect.rebuildRoute) requestOsrmRoute(fromNavigation = true)
                }
            }
        }
    }

    private fun refreshPlaces(location: GeoLocation, force: Boolean = false) {
        if (!force && placesJob?.isActive == true) return
        placesJob?.cancel()
        placesJob = viewModelScope.launch {
            _state.update { it.copy(isLoadingPlaces = true, errorMessage = null) }
            try {
                runCatching { ensureAnonymousUser() }
                val nearby = getNearbyPlaces(location)
                val selectedId = _state.value.selectedPlaceId
                getTourProgress.bindStops(
                    _state.value.routePlaces.map { it.place }.ifEmpty { nearby },
                )
                selectedId?.let(getTourProgress::selectStop)
                _state.update {
                    val selectedStillVisible = nearby.any { place -> place.id == it.selectedPlaceId } ||
                        it.routePlaces.any { place -> place.id == it.selectedPlaceId }
                    val nextSelected = if (selectedStillVisible) it.selectedPlaceId else null
                    val selectedPlace = nearby.firstOrNull { place -> place.id == nextSelected }
                        ?: it.routePlaces.firstOrNull { place -> place.id == nextSelected }?.place
                    it.copy(
                        places = nearby,
                        selectedPlaceId = nextSelected,
                        placeDetails = selectedPlace?.let(getPlaceDetails::invoke),
                        guideNarration = selectedPlace?.let(getAiGuide::invoke),
                        isGuideOpen = if (selectedStillVisible) it.isGuideOpen else false,
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

    private companion object {
        const val PLAYBACK_TICK_MS = 500L
    }
}

package com.example.homework.ui.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homework.R
import com.example.homework.core.domain.navigation.WalkingNavigation
import com.example.homework.core.locale.AppLanguage
import com.example.homework.core.locale.AppStrings
import com.example.homework.core.locale.LocaleStore
import com.example.homework.core.domain.usecase.ControlAiGuideUseCase
import com.example.homework.core.domain.usecase.BuildAdventureRouteUseCase
import com.example.homework.core.domain.usecase.EnsureAnonymousUserUseCase
import com.example.homework.core.domain.usecase.GetAiGuideUseCase
import com.example.homework.core.domain.usecase.GetHistoricalPlaceDetailsUseCase
import com.example.homework.core.domain.usecase.GetHistoricalPlacesUseCase
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
import com.example.homework.core.domain.usecase.RebuildAdventureRouteUseCase
import com.example.homework.entity.map.GeoLocation
import com.example.homework.entity.map.KazanCenter
import com.example.homework.entity.map.NavigationStatus
import com.example.homework.entity.map.OsmPlace
import com.example.homework.entity.map.PlaceFilter
import com.example.homework.entity.map.TransportMode
import com.example.homework.entity.map.YearRange
import com.example.homework.entity.map.addRoutePlace
import com.example.homework.entity.map.distanceMeters
import com.example.homework.entity.map.removeRoutePlace
import com.example.homework.entity.map.updateRoutePlaceVisitDuration
import com.example.homework.entity.place.HistoricalPlaceDetails
import com.example.homework.entity.place.PlaceDetails
import com.example.homework.entity.tour.BuilderView
import com.example.homework.entity.tour.KazanTime
import com.example.homework.entity.tour.RouteStatus
import com.example.homework.entity.tour.WalkPace
import com.example.homework.entity.guide.SpeechStatus
import com.example.homework.core.data.source.guide.RouteSummarySource
import com.example.homework.core.data.source.place.PlaceImageSource
import com.example.homework.core.domain.repository.VoiceReadingRepository
import com.example.homework.entity.tour.toRoutePlaces
import com.example.homework.ui.feature.map.state.LiveMapUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class LiveMapViewModel(
    private val getNearbyPlaces: GetNearbyPlacesUseCase,
    private val getHistoricalPlaces: GetHistoricalPlacesUseCase,
    private val getHistoricalPlaceDetails: GetHistoricalPlaceDetailsUseCase,
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
    private val buildAdventureRoute: BuildAdventureRouteUseCase,
    private val rebuildAdventureRoute: RebuildAdventureRouteUseCase,
    private val placeImageSource: PlaceImageSource,
    private val voiceReadingRepository: VoiceReadingRepository,
    private val routeSummarySource: RouteSummarySource,
    private val strings: AppStrings,
    private val localeStore: LocaleStore,
) : ViewModel() {
    private val _state = MutableStateFlow(
        LiveMapUiState(permissionGranted = getUserLocation.hasPermission()),
    )
    val state: StateFlow<LiveMapUiState> = _state.asStateFlow()

    private val navigator = WalkingNavigation(strings)
    private var locationJob: Job? = null
    private var placesJob: Job? = null
    private var historicalJob: Job? = null
    private var routeJob: Job? = null
    private var adventureJob: Job? = null
    private var corridorImageJob: Job? = null
    private var navigationGuideJob: Job? = null
    private var guideAudioJob: Job? = null
    private var summaryAudioJob: Job? = null
    private var didCenterOnUser = false
    private var didReloadAroundUser = false

    init {
        viewModelScope.launch {
            observeAiGuidePlayback().collect { playback ->
                _state.update { it.copy(guidePlayback = playback) }
            }
        }
        viewModelScope.launch {
            routeSummarySource.status.collect { status ->
                _state.update { it.copy(routeSummarySpeech = status) }
            }
        }
        viewModelScope.launch {
            routeSummarySource.completed.collect { finished ->
                _state.update { it.copy(routeSummaryFinished = finished) }
            }
        }
        viewModelScope.launch {
            getTourProgress.observe().collect { progress ->
                _state.update { it.copy(tourProgress = progress) }
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(PLAYBACK_TICK_MS.milliseconds)
                if (_state.value.guidePlayback.isPlaying) {
                    controlAiGuide.tick()
                }
            }
        }
        viewModelScope.launch {
            runCatching { ensureAnonymousUser() }
            refreshPlaces()
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
        if (filter == PlaceFilter.History) {
            refreshHistoricalPlaces()
        }
    }

    fun setHistoricalMap(enabled: Boolean) {
        setPlaceFilter(if (enabled) PlaceFilter.History else PlaceFilter.All)
    }

    fun setHistoricalYearRange(range: YearRange) {
        if (range == _state.value.historicalYearRange) return
        _state.update { it.copy(historicalYearRange = range) }
        refreshHistoricalPlaces()
    }

    fun toggleRoutePanel() {
        _state.update { it.copy(routePanelExpanded = !it.routePanelExpanded) }
    }

    fun openRouteBuilder() {
        val current = _state.value
        if (current.builderView == BuilderView.Planner || current.routeStatus == RouteStatus.Success) {
            _state.update { it.copy(builderView = BuilderView.Planner, isPlannerExpanded = true) }
        } else {
            _state.update { it.copy(builderView = BuilderView.Form, isPlannerExpanded = false) }
        }
    }

    fun setDurationMinutes(minutes: Int) {
        _state.update {
            it.copy(formState = it.formState.copy(durationMinutes = minutes.coerceIn(30, 720)))
        }
    }

    fun toggleInterest(interest: String) {
        _state.update { state ->
            val current = state.formState.interests
            val next = if (interest in current) current - interest else current + interest
            state.copy(formState = state.formState.copy(interests = next))
        }
    }

    fun setPace(pace: WalkPace) {
        _state.update { it.copy(formState = it.formState.copy(pace = pace)) }
    }

    fun useMyLocationAsStart() {
        _state.update {
            it.copy(
                customStart = null,
                draftStart = null,
                recenterToken = it.recenterToken + 1,
            )
        }
        if (_state.value.routePlaces.isNotEmpty()) {
            refreshOsrmGeometry()
        }
    }

    fun beginPickStart() {
        _state.update {
            val origin = if (it.builderView == BuilderView.PickStart) {
                it.pickStartReturnView
            } else {
                it.builderView
            }
            it.copy(
                builderView = BuilderView.PickStart,
                pickStartReturnView = origin,
                draftStart = it.customStart ?: KazanCenter,
                recenterToken = it.recenterToken + 1,
            )
        }
    }

    fun setDraftStart(location: GeoLocation) {
        _state.update { it.copy(draftStart = location) }
    }

    fun cancelPickStart() {
        _state.update {
            it.copy(
                builderView = it.pickStartReturnView,
                draftStart = null,
            )
        }
    }

    fun confirmPickStart() {
        val start = _state.value.draftStart ?: KazanCenter
        val returnView = _state.value.pickStartReturnView
        _state.update {
            it.copy(
                customStart = start,
                draftStart = null,
                builderView = returnView,
                recenterToken = it.recenterToken + 1,
            )
        }
        refreshPlaces(around = start, force = true)
        if (_state.value.routePlaces.isNotEmpty()) {
            refreshOsrmGeometry()
        }
    }

    fun setAiRequest(value: String) {
        _state.update { it.copy(formState = it.formState.copy(aiRequest = value)) }
    }

    fun setRebuildPrompt(value: String) {
        _state.update { it.copy(rebuildPrompt = value.take(1_000)) }
    }

    fun toggleParamsExpanded() {
        _state.update { it.copy(paramsExpanded = !it.paramsExpanded) }
    }

    fun submitAdventureRoute() {
        val form = _state.value.formState
        if (!form.isValid) {
            _state.update { it.copy(errorMessage = strings.get(R.string.builder_validation)) }
            return
        }
        _state.update {
            it.copy(
                builderView = BuilderView.Planner,
                isPlannerExpanded = true,
                paramsExpanded = false,
                errorMessage = null,
            )
        }
        requestAdventureCreate()
    }

    fun retryAdventureRoute() = requestAdventureCreate()

    fun rebuildAdventure() {
        val prompt = _state.value.rebuildPrompt.trim()
        if (prompt.length !in 1..1_000) {
            _state.update { it.copy(errorMessage = strings.get(R.string.builder_rebuild_validation)) }
            return
        }
        val remaining = if (navigator.state.isActive) {
            navigator.remainingPlaces().map { it.location }
        } else {
            _state.value.routePlaces.map { it.location }
        }
        if (remaining.isEmpty()) {
            _state.update { it.copy(errorMessage = strings.get(R.string.builder_rebuild_empty)) }
            return
        }
        val remainingIds = if (navigator.state.isActive) {
            navigator.remainingPlaces().map { it.id }.toSet()
        } else {
            _state.value.routePlaces.map { it.id }.toSet()
        }
        val visited = _state.value.routePlaces
            .filter { it.id !in remainingIds }
            .map { it.location }
        if (adventureJob?.isActive == true) return
        adventureJob = viewModelScope.launch {
            _state.update {
                it.copy(routeStatus = RouteStatus.Loading, errorMessage = null)
            }
            try {
                runCatching { ensureAnonymousUser() }
                val rebuilt = rebuildAdventureRoute(
                    userLocation = _state.value.routeStart,
                    visitedPlaces = visited,
                    remainingPlaces = remaining,
                    aiRequest = prompt,
                    currentAt = KazanTime.nowIso(),
                )
                applyAdventureResult(rebuilt)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        routeStatus = RouteStatus.Error,
                        errorMessage = e.message ?: strings.get(R.string.error_build_route),
                    )
                }
            }
        }
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
        val user = _state.value.routeStart
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
                        errorMessage = e.message ?: strings.get(R.string.error_optimize_route),
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
        _state.update { it.copy(routePanelExpanded = false, selectedPlaceId = null, isGuideOpen = false, showArrivalDialog = false) }
        if (effect.rebuildRoute) requestOsrmRoute(fromNavigation = true)
        if (_state.value.isAiGuideEnabled) startNavigationGuide()
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
        if (!navigator.state.isActive) {
            stopNavigationGuide()
        } else if (_state.value.isAiGuideEnabled) {
            startNavigationGuide()
        }
    }

    fun stopNavigation() {
        navigator.stop()
        publishNavigation()
        stopNavigationGuide()
        _state.update { it.copy(showArrivalDialog = false) }
    }

    fun dismissArrivalDialog() {
        _state.update { it.copy(showArrivalDialog = false) }
        stopNavigation()
    }

    fun toggleAiGuide() {
        val enabled = !_state.value.isAiGuideEnabled
        _state.update { it.copy(isAiGuideEnabled = enabled) }
        if (enabled) {
            startNavigationGuide()
        } else {
            stopNavigationGuide()
        }
    }

    fun openGuide() {
        val place = _state.value.selectedPlace ?: return
        _state.update { it.copy(isGuideOpen = true) }
        playPlaceNarration(place, autoplay = true)
    }

    fun closeGuide() {
        _state.update { it.copy(isGuideOpen = false) }
    }

    fun dismissRouteSummary() {
        routeSummarySource.pause()
        _state.update { it.copy(isRouteSummaryVisible = false, isRouteSummaryExpanded = false) }
    }

    fun showRouteSummary() {
        if (_state.value.routeSummary.isBlank()) return
        _state.update { it.copy(isRouteSummaryVisible = true) }
    }

    fun toggleRouteSummaryExpanded() {
        _state.update { it.copy(isRouteSummaryExpanded = !it.isRouteSummaryExpanded) }
    }

    fun toggleRouteSummarySpeech() {
        when (_state.value.routeSummarySpeech) {
            SpeechStatus.Idle, SpeechStatus.Loading -> return
            SpeechStatus.Error -> prefetchSummaryAudio(_state.value.routeSummary)
            SpeechStatus.Ready, SpeechStatus.Playing, SpeechStatus.Paused -> {
                if (_state.value.guidePlayback.isPlaying) controlAiGuide.togglePlayback()
                routeSummarySource.togglePlayback()
            }
        }
    }

    fun retryRouteSummarySpeech() = prefetchSummaryAudio(_state.value.routeSummary)

    fun toggleGuidePlayback() = controlAiGuide.togglePlayback()

    fun seekGuide(progress: Float) = controlAiGuide.seek(progress)

    fun toggleGuideSound() = controlAiGuide.toggleMute()

    fun goToNextStop() = moveToStop(goToNextStopUseCase(), keepGuide = _state.value.isGuideOpen)

    fun goToPreviousStop() = moveToStop(goToPreviousStopUseCase(), keepGuide = true)

    fun markPlaceVisited() = moveToStop(markPlaceVisitedUseCase(), keepGuide = false)

    fun retryPlaces() {
        refreshPlaces(around = _state.value.user, force = true)
    }

    private fun requestAdventureCreate() {
        if (adventureJob?.isActive == true) return
        adventureJob = viewModelScope.launch {
            _state.update {
                it.copy(routeStatus = RouteStatus.Loading, errorMessage = null)
            }
            try {
                runCatching { ensureAnonymousUser() }
                val form = _state.value.formState
                val created = buildAdventureRoute(
                    form.toRequest(
                        location = _state.value.routeStart,
                        startAt = KazanTime.nowIso(),
                    ),
                )
                applyAdventureResult(created)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        routeStatus = RouteStatus.Error,
                        errorMessage = e.message ?: strings.get(R.string.error_build_route),
                    )
                }
            }
        }
    }

    private fun applyAdventureResult(route: com.example.homework.entity.tour.AdventureRoute) {
        val places = route.toRoutePlaces()
        if (places.isEmpty()) {
            _state.update {
                it.copy(
                    adventure = route,
                    routePlaces = emptyList(),
                    routeStatus = RouteStatus.Error,
                    errorMessage = strings.get(R.string.error_build_route),
                )
            }
            bindRouteSummary(null)
            return
        }
        getTourProgress.bindStops(places.map { it.place })
        _state.update {
            it.copy(
                routePlaces = places,
                adventure = route,
                routeStatus = RouteStatus.Success,
                transportMode = TransportMode.Walking,
                routePanelExpanded = true,
                paramsExpanded = false,
                errorMessage = null,
                placeFilter = PlaceFilter.All,
            )
        }
        bindRouteSummary(route.summary)
        refreshOsrmGeometry()
        if (_state.value.places.isEmpty()) {
            refreshPlaces(_state.value.routeStart)
        }
    }

    private fun refreshOsrmGeometry() {
        routeJob?.cancel()
        requestOsrmRoute(fromNavigation = false)
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
        routeJob?.cancel()
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
                applyCorridorImages()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val message = e.message ?: strings.get(R.string.error_build_route)
                _state.update { it.copy(isBuildingRoute = false, errorMessage = message) }
                if (fromNavigation) {
                    navigator.onRouteFailed(message)
                    publishNavigation()
                }
            }
        }
    }

    private fun buildRoutePoints(fromNavigation: Boolean): List<GeoLocation> {
        val start = _state.value.routeStart
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
                playPlaceNarration(place, autoplay = true)
            }
        }
    }

    private fun openPlace(placeId: String?, openGuide: Boolean) {
        if (placeId == null) {
            guideAudioJob?.cancel()
            _state.update {
                it.copy(
                    selectedPlaceId = null,
                    placeDetails = null,
                    guideNarration = null,
                    isGuideOpen = false,
                    isPreparingGuideAudio = false,
                    isLoadingHistoricalDetails = false,
                )
            }
            return
        }
        val rawPlace = _state.value.places.firstOrNull { it.id == placeId }
            ?: _state.value.routePlaces.firstOrNull { it.id == placeId }?.place
            ?: return
        val place = rawPlace
        val historicalDetails = if (place.isHistorical) getHistoricalPlaceDetails.cached(place.id) else null
        val displayPlace = historicalDetails?.let { enrichHistoricalPlace(place, it) } ?: place
        val loadingHistorical = place.isHistorical &&
            historicalDetails == null &&
            WIKIDATA_ID.matches(place.id)
        getTourProgress.selectStop(placeId)
        if (_state.value.selectedPlaceId != placeId) {
            guideAudioJob?.cancel()
        }
        controlAiGuide.resetForPlace()
        _state.update {
            it.copy(
                selectedPlaceId = placeId,
                placeDetails = if (loadingHistorical) null else placeDetailsFor(displayPlace, historicalDetails),
                guideNarration = if (loadingHistorical) null else getAiGuide(displayPlace),
                isGuideOpen = openGuide,
                isPreparingGuideAudio = false,
                isLoadingHistoricalDetails = loadingHistorical,
            )
        }
        if (place.imageUrl.isNullOrBlank() && _state.value.mapPlaces.any { it.id == place.id }) {
            viewModelScope.launch { applyUniqueImage(place.id) }
        }
        if (place.isHistorical) {
            if (loadingHistorical) {
                viewModelScope.launch {
                    val details = runCatching { getHistoricalPlaceDetails(place.id) }.getOrNull()
                    if (_state.value.selectedPlaceId != place.id) return@launch
                    if (details == null) {
                        _state.update { it.copy(isLoadingHistoricalDetails = false) }
                        return@launch
                    }
                    applyHistoricalDetails(place.id, details)
                    applyUniqueImage(place.id)
                }
            }
            return
        }
    }

    private fun startLocationUpdates() {
        if (locationJob?.isActive == true) return
        locationJob = viewModelScope.launch {
            getUserLocation().collect { location ->
                val shouldCenter = !didCenterOnUser
                if (shouldCenter) didCenterOnUser = true
                _state.update {
                    val skipRecenter = it.customStart != null || it.builderView == BuilderView.PickStart
                    it.copy(
                        user = location,
                        permissionGranted = true,
                        recenterToken = if (shouldCenter && !skipRecenter) {
                            it.recenterToken + 1
                        } else {
                            it.recenterToken
                        },
                    )
                }
                if (!didReloadAroundUser) {
                    didReloadAroundUser = true
                    refreshPlaces(around = _state.value.routeStart, force = true)
                }
                if (navigator.state.isActive) {
                    val previousStatus = navigator.state.status
                    val effect = navigator.onLocation(location, System.currentTimeMillis())
                    publishNavigation()
                    if (effect.rebuildRoute) requestOsrmRoute(fromNavigation = true)
                    if (effect.arrivedAtLastStop) {
                        _state.update { it.copy(showArrivalDialog = true) }
                    }
                    if (
                        _state.value.isAiGuideEnabled &&
                        previousStatus != NavigationStatus.Arrived &&
                        navigator.state.status == NavigationStatus.Arrived
                    ) {
                        startNavigationGuide()
                    }
                }
            }
        }
    }

    private fun refreshPlaces(around: GeoLocation? = null, force: Boolean = false) {
        if (!force && placesJob?.isActive == true) return
        placesJob?.cancel()
        historicalJob?.cancel()
        placesJob = viewModelScope.launch {
            _state.update { it.copy(isLoadingPlaces = true, errorMessage = null) }
            try {
                runCatching { ensureAnonymousUser() }
                val years = _state.value.historicalYearRange
                val (nearby, historicalResult) = coroutineScope {
                    val nearbyDeferred = async { loadCityPlaces(around) }
                    val historicalDeferred = async { loadHistoricalPlaces(around, years) }
                    nearbyDeferred.await() to historicalDeferred.await()
                }
                val historical = historicalResult.getOrDefault(emptyList())
                val merged = mergePlaces(nearby, historical)
                val selectedId = _state.value.selectedPlaceId
                getTourProgress.bindStops(
                    _state.value.routePlaces.map { it.place }.ifEmpty { merged },
                )
                selectedId?.let(getTourProgress::selectStop)
                val historicalError = historicalErrorMessage(nearby, historicalResult)
                _state.update {
                    it.withPlaces(merged).copy(
                        isLoadingPlaces = false,
                        errorMessage = historicalError,
                    )
                }
                if (_state.value.historicalYearRange != years) {
                    refreshHistoricalPlaces()
                }
                applyCorridorImages()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoadingPlaces = false,
                        errorMessage = e.message ?: strings.get(R.string.error_load_places),
                    )
                }
            }
        }
    }

    private fun refreshHistoricalPlaces() {
        historicalJob?.cancel()
        historicalJob = viewModelScope.launch {
            val years = _state.value.historicalYearRange
            val nearby = _state.value.places.filter { !it.isHistorical }
            val historicalResult = loadHistoricalPlaces(_state.value.routeStart, years)
            val merged = mergePlaces(nearby, historicalResult.getOrDefault(emptyList()))
            _state.update { state ->
                val historicalError = historicalErrorMessage(nearby, historicalResult)
                state.withPlaces(merged).copy(
                    errorMessage = when {
                        historicalError != null -> historicalError
                        state.errorMessage == strings.get(R.string.error_load_historical_places) -> null
                        else -> state.errorMessage
                    },
                )
            }
            applyCorridorImages()
        }
    }

    private suspend fun loadCityPlaces(around: GeoLocation?): List<OsmPlace> {
        val city = getNearbyPlaces(KazanCenter)
        val extras = around
            ?.takeIf { location ->
                distanceMeters(
                    location.lat,
                    location.lon,
                    KazanCenter.lat,
                    KazanCenter.lon,
                ) >= USER_AREA_MERGE_METERS
            }
            ?.let { location -> runCatching { getNearbyPlaces(location) }.getOrDefault(emptyList()) }
            .orEmpty()
        return (city + extras).distinctBy { it.id }
    }

    private suspend fun loadHistoricalPlaces(
        around: GeoLocation?,
        years: YearRange,
    ): Result<List<OsmPlace>> {
        val origin = around ?: _state.value.routeStart
        val localizedName = strings.get(R.string.historical_place_name)
        return runCatching {
            getHistoricalPlaces(origin, HISTORICAL_RADIUS_METERS, years)
                .map { place ->
                    if (place.isHistorical) place.copy(name = localizedName) else place
                }
        }
    }

    private fun mergePlaces(nearby: List<OsmPlace>, historical: List<OsmPlace>): List<OsmPlace> =
        (nearby + historical).distinctBy { it.id }.map { place ->
            if (!place.isHistorical) {
                place
            } else {
                getHistoricalPlaceDetails.cached(place.id)
                    ?.let { details -> enrichHistoricalPlace(place, details) }
                    ?: place
            }
        }

    private fun applyHistoricalDetails(placeId: String, details: HistoricalPlaceDetails) {
        _state.update { state ->
            val updatedPlaces = state.places.map { place ->
                if (place.id == placeId) enrichHistoricalPlace(place, details) else place
            }
            val updatedRoute = state.routePlaces.map { item ->
                if (item.id == placeId) item.copy(place = enrichHistoricalPlace(item.place, details)) else item
            }
            val selected = updatedPlaces.firstOrNull { it.id == placeId }
                ?: updatedRoute.firstOrNull { it.id == placeId }?.place
                ?: return@update state
            state.copy(
                places = updatedPlaces,
                routePlaces = updatedRoute,
                placeDetails = placeDetailsFor(selected, details),
                guideNarration = getAiGuide(selected),
                isLoadingHistoricalDetails = false,
            )
        }
    }

    private fun enrichHistoricalPlace(place: OsmPlace, details: HistoricalPlaceDetails): OsmPlace =
        place.copy(
            name = historicalDisplayName(details),
            description = details.story,
            imageUrl = details.imageUrl ?: place.imageUrl,
        )

    private fun placeDetailsFor(place: OsmPlace, historical: HistoricalPlaceDetails?): PlaceDetails {
        val base = getPlaceDetails(place)
        if (historical == null) return base
        val short = historical.story.substringBefore('.').trim()
        return base.copy(
            name = historicalDisplayName(historical),
            imageUrl = historical.imageUrl ?: base.imageUrl,
            fullDescription = historical.story.ifBlank { base.fullDescription },
            shortDescription = if (short.isNotBlank()) "$short." else base.shortDescription,
            firstMentionYear = historical.firstMentionYear,
        )
    }

    private fun historicalDisplayName(details: HistoricalPlaceDetails): String {
        val tatar = details.nameTt?.trim().orEmpty()
        return if (localeStore.get() == AppLanguage.Tatar && tatar.isNotEmpty()) {
            tatar
        } else {
            details.nameRu.ifBlank { details.placeId }
        }
    }

    private fun historicalErrorMessage(
        nearby: List<OsmPlace>,
        historicalResult: Result<List<OsmPlace>>,
    ): String? {
        if (nearby.isNotEmpty() || historicalResult.isSuccess) return null
        return strings.get(R.string.error_load_historical_places)
    }

    private fun LiveMapUiState.withPlaces(nextPlaces: List<OsmPlace>): LiveMapUiState {
        val selectedStillVisible = nextPlaces.any { place -> place.id == selectedPlaceId } ||
            routePlaces.any { place -> place.id == selectedPlaceId }
        val nextSelected = if (selectedStillVisible) selectedPlaceId else null
        val selectedPlace = nextPlaces.firstOrNull { place -> place.id == nextSelected }
            ?: routePlaces.firstOrNull { place -> place.id == nextSelected }?.place
        val historical = selectedPlace
            ?.takeIf { it.isHistorical }
            ?.let { getHistoricalPlaceDetails.cached(it.id) }
        val loadingHistorical = selectedPlace?.isHistorical == true &&
            historical == null &&
            isLoadingHistoricalDetails
        return copy(
            places = nextPlaces,
            selectedPlaceId = nextSelected,
            placeDetails = if (loadingHistorical) null else selectedPlace?.let { placeDetailsFor(it, historical) },
            guideNarration = if (loadingHistorical) null else selectedPlace?.let(getAiGuide::invoke),
            isGuideOpen = if (selectedStillVisible) isGuideOpen else false,
            isPreparingGuideAudio = if (selectedStillVisible) isPreparingGuideAudio else false,
            isLoadingHistoricalDetails = loadingHistorical,
        )
    }

    private fun applyCorridorImages() {
        val targets = _state.value.mapPlaces.filter { it.imageUrl.isNullOrBlank() }
        if (targets.isEmpty()) return
        corridorImageJob?.cancel()
        corridorImageJob = viewModelScope.launch {
            val updates = coroutineScope {
                targets.map { place ->
                    async {
                        val url = runCatching { placeImageSource.findImageUrl(place) }.getOrNull()
                        place.id to url
                    }
                }.awaitAll()
            }.mapNotNull { (id, url) -> url?.let { id to it } }.toMap()
            if (updates.isEmpty()) return@launch
            _state.update { state ->
                val places = state.places.map { place ->
                    val url = updates[place.id] ?: return@map place
                    place.copy(imageUrl = url)
                }
                val selected = state.selectedPlaceId
                val selectedPlace = places.firstOrNull { it.id == selected }
                    ?: state.routePlaces.firstOrNull { it.id == selected }?.place
                val details = state.placeDetails
                state.copy(
                    places = places,
                    placeDetails = if (
                        selectedPlace != null &&
                        details != null &&
                        details.placeId == selected
                    ) {
                        details.copy(imageUrl = selectedPlace.imageUrl)
                    } else {
                        details
                    },
                )
            }
        }
    }

    private suspend fun applyUniqueImage(placeId: String) {
        val place = _state.value.places.firstOrNull { it.id == placeId }
            ?: _state.value.routePlaces.firstOrNull { it.id == placeId }?.place
            ?: return
        if (!place.imageUrl.isNullOrBlank()) return
        val url = runCatching { placeImageSource.findImageUrl(place) }.getOrNull() ?: return
        _state.update { state ->
            if (state.selectedPlaceId != placeId) return@update state
            val places = state.places.map { item ->
                if (item.id == placeId) item.copy(imageUrl = url) else item
            }
            val routePlaces = state.routePlaces.map { item ->
                if (item.id == placeId) item.copy(place = item.place.copy(imageUrl = url)) else item
            }
            val details = state.placeDetails
            state.copy(
                places = places,
                routePlaces = routePlaces,
                placeDetails = if (details?.placeId == placeId) details.copy(imageUrl = url) else details,
            )
        }
    }

    private fun startNavigationGuide() {
        val place = currentNavigationPlace() ?: return
        controlAiGuide.resetForPlace()
        _state.update { it.copy(guideNarration = getAiGuide(place)) }
        playPlaceNarration(place, autoplay = true) { _state.value.isAiGuideEnabled }
    }

    private fun stopNavigationGuide() {
        guideAudioJob?.cancel()
        navigationGuideJob?.cancel()
        _state.update { it.copy(isPreparingGuideAudio = false) }
        if (_state.value.guidePlayback.isPlaying) {
            controlAiGuide.togglePlayback()
        }
    }

    private fun playPlaceNarration(
        place: OsmPlace,
        autoplay: Boolean,
        stillEnabled: () -> Boolean = { true },
    ) {
        routeSummarySource.pause()
        guideAudioJob?.cancel()
        guideAudioJob = viewModelScope.launch {
            _state.update { it.copy(isPreparingGuideAudio = true) }
            val result = runCatching { getAiGuide.prepareAudio(place) }
                .onFailure { error -> if (error is CancellationException) throw error }
            if (!isActive) return@launch
            val samePlace = _state.value.selectedPlaceId == place.id ||
                currentNavigationPlace()?.id == place.id
            if (!samePlace) {
                _state.update { it.copy(isPreparingGuideAudio = false) }
                return@launch
            }
            val enabled = stillEnabled()
            _state.update {
                it.copy(
                    isPreparingGuideAudio = false,
                    guideNarration = getAiGuide(place),
                    errorMessage = if (result.isFailure) {
                        result.exceptionOrNull()?.message ?: strings.get(R.string.guide_audio_error)
                    } else {
                        it.errorMessage
                    },
                )
            }
            if (result.isFailure || !enabled) return@launch
            if (autoplay && !_state.value.guidePlayback.isPlaying) {
                controlAiGuide.togglePlayback()
            }
        }
    }

    private fun bindRouteSummary(raw: String?) {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) {
            summaryAudioJob?.cancel()
            routeSummarySource.reset()
            _state.update {
                it.copy(
                    routeSummary = "",
                    isRouteSummaryVisible = false,
                    isRouteSummaryExpanded = false,
                    routeSummarySpeech = SpeechStatus.Idle,
                    routeSummaryFinished = false,
                )
            }
            return
        }
        val reuse = text == _state.value.routeSummary && routeSummarySource.hasPrepared(text)
        _state.update {
            it.copy(
                routeSummary = text,
                isRouteSummaryVisible = true,
                isRouteSummaryExpanded = false,
            )
        }
        if (!reuse) prefetchSummaryAudio(text)
    }

    private fun prefetchSummaryAudio(text: String) {
        if (text.isBlank()) return
        summaryAudioJob?.cancel()
        routeSummarySource.reset()
        routeSummarySource.setLoading()
        summaryAudioJob = viewModelScope.launch {
            val result = runCatching {
                val wav = voiceReadingRepository.synthesizeAll(text)
                routeSummarySource.prepare(text, wav)
            }.onFailure { error -> if (error is CancellationException) throw error }
            if (!isActive) return@launch
            if (result.isFailure) routeSummarySource.setError()
        }
    }

    override fun onCleared() {
        summaryAudioJob?.cancel()
        routeSummarySource.release()
        super.onCleared()
    }

    private fun currentNavigationPlace(): OsmPlace? =
        _state.value.routePlaces.getOrNull(_state.value.navigation.placeIndex)?.place

    private companion object {
        const val PLAYBACK_TICK_MS = 500L
        const val USER_AREA_MERGE_METERS = 1_500
        const val HISTORICAL_RADIUS_METERS = 10_000
        val WIKIDATA_ID = Regex("^Q\\d+$")
    }
}

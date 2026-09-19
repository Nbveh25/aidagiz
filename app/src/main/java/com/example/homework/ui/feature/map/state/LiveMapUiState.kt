package com.example.homework.ui.feature.map.state

import com.example.homework.entity.guide.AiGuideNarration
import com.example.homework.entity.guide.AiGuidePlayback
import com.example.homework.entity.map.GeoLocation
import com.example.homework.entity.map.KazanCenter
import com.example.homework.entity.map.NavigationState
import com.example.homework.entity.map.OsmPlace
import com.example.homework.entity.map.PlaceFilter
import com.example.homework.entity.map.RoutePlace
import com.example.homework.entity.map.RouteResult
import com.example.homework.entity.map.TransportMode
import com.example.homework.entity.map.YearRange
import com.example.homework.entity.map.buildYandexMapsRouteUrl
import com.example.homework.entity.map.filterPlaces
import com.example.homework.entity.place.PlaceDetails
import com.example.homework.entity.tour.AdventureRoute
import com.example.homework.entity.tour.BuilderView
import com.example.homework.entity.tour.RouteFormState
import com.example.homework.entity.tour.RouteStatus
import com.example.homework.entity.tour.TourProgress

data class LiveMapUiState(
    val user: GeoLocation? = null,
    val places: List<OsmPlace> = emptyList(),
    val selectedPlaceId: String? = null,
    val placeDetails: PlaceDetails? = null,
    val guideNarration: AiGuideNarration? = null,
    val guidePlayback: AiGuidePlayback = AiGuidePlayback(),
    val tourProgress: TourProgress = TourProgress(),
    val isGuideOpen: Boolean = false,
    val isLoadingPlaces: Boolean = false,
    val errorMessage: String? = null,
    val permissionGranted: Boolean = false,
    val recenterToken: Int = 0,
    val placeFilter: PlaceFilter = PlaceFilter.All,
    val routePlaces: List<RoutePlace> = emptyList(),
    val transportMode: TransportMode = TransportMode.Walking,
    val route: RouteResult? = null,
    val routeDirty: Boolean = false,
    val isBuildingRoute: Boolean = false,
    val fitRouteToken: Int = 0,
    val routePanelExpanded: Boolean = false,
    val navigation: NavigationState = NavigationState(),
    val builderView: BuilderView = BuilderView.Form,
    val routeStatus: RouteStatus = RouteStatus.Idle,
    val formState: RouteFormState = RouteFormState(),
    val isPlannerExpanded: Boolean = false,
    val paramsExpanded: Boolean = false,
    val adventure: AdventureRoute? = null,
    val rebuildPrompt: String = "",
    val historicalYearRange: YearRange = YearRange.Default,
    val isLoadingHistoricalDetails: Boolean = false,
) {
    val selectedPlace: OsmPlace?
        get() = places.firstOrNull { it.id == selectedPlaceId }
            ?: routePlaces.firstOrNull { it.id == selectedPlaceId }?.place

    val mapCenter: GeoLocation
        get() = user ?: KazanCenter

    val filteredPlaces: List<OsmPlace>
        get() = filterPlaces(places, placeFilter)

    val selectedInRoute: Boolean
        get() = selectedPlaceId != null && routePlaces.any { it.id == selectedPlaceId }

    val followUser: Boolean
        get() = navigation.isActive && navigation.followUser

    val yandexUrl: String
        get() = buildYandexMapsRouteUrl(
            points = listOf(mapCenter) + routePlaces.map { it.location },
            mode = transportMode,
        )
}

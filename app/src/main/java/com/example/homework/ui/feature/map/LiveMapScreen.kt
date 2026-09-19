package com.example.homework.ui.feature.map

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.homework.R
import com.example.homework.core.locale.AppLanguage
import com.example.homework.core.locale.LocaleStore
import com.example.homework.entity.map.GeoLocation
import com.example.homework.entity.map.KazanCenter
import com.example.homework.entity.map.OsmPlace
import com.example.homework.entity.map.PlaceCategory
import com.example.homework.entity.map.PlaceFilter
import com.example.homework.entity.map.RoutePlace
import com.example.homework.entity.map.RouteResult
import com.example.homework.entity.map.TransportMode
import com.example.homework.entity.map.YearRange
import com.example.homework.ui.feature.map.state.LiveMapUiState
import com.example.homework.ui.locale.localizedPlacesCount
import com.example.homework.ui.uikit.component.LanguageToggle
import com.example.homework.ui.uikit.component.RecenterChip
import com.example.homework.ui.uikit.component.StatusChip
import com.example.homework.ui.uikit.theme.ForestGreen
import com.example.homework.ui.uikit.theme.HomeworkTheme
import com.example.homework.ui.uikit.theme.TextOnForest
import com.example.homework.ui.uikit.theme.TextPrimary
import com.example.homework.ui.uikit.theme.TextSecondary
import org.koin.androidx.compose.koinViewModel

@Composable
fun LiveMapScreen(
    modifier: Modifier = Modifier,
    viewModel: LiveMapViewModel = koinViewModel(),
    language: AppLanguage? = null,
    onLanguageSelect: ((AppLanguage) -> Unit)? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val localeStore = remember { LocaleStore(context) }
    val fallbackLanguage = remember { localeStore.get() }
    val resolvedLanguage = language ?: fallbackLanguage
    val resolveLanguageSelect = onLanguageSelect ?: { selected ->
        if (selected != resolvedLanguage) {
            localeStore.set(selected)
            (context as? Activity)?.recreate()
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        viewModel.onPermissionResult(grants.values.any { it })
    }

    LaunchedEffect(Unit) {
        if (!state.permissionGranted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    LiveMapContent(
        state = state,
        onPlaceSelected = viewModel::selectPlace,
        onUserMapInteraction = viewModel::onUserMapInteraction,
        onSetPlaceFilter = viewModel::setPlaceFilter,
        onSetHistoricalYearRange = viewModel::setHistoricalYearRange,
        onAllowPermission = {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        },
        onOpenSettings = {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                ),
            )
        },
        onRetryPlaces = viewModel::retryPlaces,
        onRecenter = viewModel::recenter,
        onPauseResumeNavigation = viewModel::pauseOrResumeNavigation,
        onRetryNavigation = viewModel::retryNavigation,
        onNextNavigationPlace = viewModel::nextNavigationPlace,
        onStopNavigation = viewModel::stopNavigation,
        onToggleRoutePanel = viewModel::toggleRoutePanel,
        onRemoveFromRoute = viewModel::removeFromRoute,
        onDurationDelta = viewModel::changeVisitDuration,
        onTransportMode = viewModel::setTransportMode,
        onBuildRoute = viewModel::buildRoute,
        onOptimizeRoute = viewModel::optimizeAndBuild,
        onOpenYandex = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(state.yandexUrl)))
        },
        onShareRoute = {
            context.startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, state.yandexUrl)
                    },
                    context.getString(R.string.share_route),
                ),
            )
        },
        onStartNavigation = viewModel::startNavigation,
        onDismissPlace = { viewModel.selectPlace(null) },
        onToggleSelectedInRoute = viewModel::toggleSelectedInRoute,
        onOpenGuide = viewModel::openGuide,
        onCloseGuide = viewModel::closeGuide,
        onMarkVisited = viewModel::markPlaceVisited,
        onTogglePlayback = viewModel::toggleGuidePlayback,
        onSeekGuide = viewModel::seekGuide,
        onToggleMute = viewModel::toggleGuideSound,
        onPreviousStop = viewModel::goToPreviousStop,
        onNextStop = viewModel::goToNextStop,
        language = resolvedLanguage,
        onLanguageSelect = resolveLanguageSelect,
        modifier = modifier,
    )
}

@Composable
fun LiveMapContent(
    state: LiveMapUiState,
    modifier: Modifier = Modifier,
    onPlaceSelected: (String?) -> Unit = {},
    onUserMapInteraction: () -> Unit = {},
    onSetPlaceFilter: (PlaceFilter) -> Unit = {},
    onSetHistoricalYearRange: (YearRange) -> Unit = {},
    onAllowPermission: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onRetryPlaces: () -> Unit = {},
    onRecenter: () -> Unit = {},
    onPauseResumeNavigation: () -> Unit = {},
    onRetryNavigation: () -> Unit = {},
    onNextNavigationPlace: () -> Unit = {},
    onStopNavigation: () -> Unit = {},
    onToggleRoutePanel: () -> Unit = {},
    onRemoveFromRoute: (String) -> Unit = {},
    onDurationDelta: (String, Int) -> Unit = { _, _ -> },
    onTransportMode: (TransportMode) -> Unit = {},
    onBuildRoute: () -> Unit = {},
    onOptimizeRoute: () -> Unit = {},
    onOpenYandex: () -> Unit = {},
    onShareRoute: () -> Unit = {},
    onStartNavigation: () -> Unit = {},
    onDismissPlace: () -> Unit = {},
    onToggleSelectedInRoute: () -> Unit = {},
    onOpenGuide: () -> Unit = {},
    onCloseGuide: () -> Unit = {},
    onMarkVisited: () -> Unit = {},
    onTogglePlayback: () -> Unit = {},
    onSeekGuide: (Float) -> Unit = {},
    onToggleMute: () -> Unit = {},
    onPreviousStop: () -> Unit = {},
    onNextStop: () -> Unit = {},
    language: AppLanguage = AppLanguage.Russian,
    onLanguageSelect: (AppLanguage) -> Unit = {},
    showLanguageToggle: Boolean = true,
    consumeStatusBars: Boolean = true,
) {
    Box(modifier = modifier.fillMaxSize()) {
        OsmMap(
            center = state.mapCenter,
            user = state.user,
            places = state.mapPlaces,
            routePlaces = state.routePlaces,
            selectedPlaceId = state.selectedPlaceId,
            routeGeometry = state.route?.geometry,
            routeDirty = state.routeDirty,
            followUser = state.followUser,
            recenterToken = state.recenterToken,
            fitRouteToken = state.fitRouteToken,
            onPlaceSelected = onPlaceSelected,
            onUserMapInteraction = onUserMapInteraction,
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (consumeStatusBars) Modifier.statusBarsPadding() else Modifier)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatusChip(
                    title = if (state.user != null) {
                        stringResource(R.string.status_here)
                    } else {
                        stringResource(R.string.status_kazan)
                    },
                    subtitle = when {
                        state.isLoadingPlaces -> stringResource(R.string.status_loading_places)
                        state.filteredPlaces.isNotEmpty() -> localizedPlacesCount(state.filteredPlaces.size)
                        else -> stringResource(R.string.status_places_empty)
                    },
                    loading = state.isLoadingPlaces,
                    modifier = Modifier.weight(1f),
                )
                if (showLanguageToggle) {
                    LanguageToggle(
                        selected = language,
                        onSelect = onLanguageSelect,
                    )
                }
            }
            if (!state.navigation.isActive) {
                PlaceFilterBar(
                    places = state.places,
                    selected = state.placeFilter,
                    onSelect = onSetPlaceFilter,
                )
                HistoricalYearBar(
                    selected = state.historicalYearRange,
                    onSelect = onSetHistoricalYearRange,
                )
            }
            if (!state.permissionGranted) {
                PermissionBanner(
                    onAllow = onAllowPermission,
                    onOpenSettings = onOpenSettings,
                )
            }
            state.errorMessage?.let { message ->
                ErrorBanner(message = message, onRetry = onRetryPlaces)
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RecenterChip(onClick = onRecenter)
            if (state.navigation.isActive) {
                NavigationHud(
                    navigation = state.navigation,
                    currentPlaceName = state.routePlaces.getOrNull(state.navigation.placeIndex)?.name,
                    onPauseResume = onPauseResumeNavigation,
                    onRecenter = onRecenter,
                    onRetry = onRetryNavigation,
                    onNext = onNextNavigationPlace,
                    onExit = onStopNavigation,
                )
            } else if (state.selectedPlace == null) {
                RouteBuilderPanel(
                    routePlaces = state.routePlaces,
                    transportMode = state.transportMode,
                    route = state.route,
                    routeDirty = state.routeDirty,
                    isBuilding = state.isBuildingRoute,
                    expanded = state.routePanelExpanded,
                    onToggleExpanded = onToggleRoutePanel,
                    onRemove = onRemoveFromRoute,
                    onDurationDelta = onDurationDelta,
                    onTransportMode = onTransportMode,
                    onBuild = onBuildRoute,
                    onOptimize = onOptimizeRoute,
                    onOpenYandex = onOpenYandex,
                    onShare = onShareRoute,
                    onStartNavigation = onStartNavigation,
                )
            }
        }

        if (!state.navigation.isActive) {
            state.selectedPlace?.let { place ->
                PlaceDetailsBottomSheet(
                    place = place,
                    details = state.placeDetails,
                    narration = state.guideNarration,
                    playback = state.guidePlayback,
                    tourProgress = state.tourProgress,
                    distanceMeters = state.user?.let { place.distanceMetersTo(it) },
                    isGuideOpen = state.isGuideOpen,
                    inRoute = state.selectedInRoute,
                    isLoadingDetails = state.isLoadingHistoricalDetails,
                    onDismiss = onDismissPlace,
                    onToggleRoute = onToggleSelectedInRoute,
                    onOpenGuide = onOpenGuide,
                    onCloseGuide = onCloseGuide,
                    onMarkVisited = onMarkVisited,
                    onTogglePlayback = onTogglePlayback,
                    onSeek = onSeekGuide,
                    onToggleMute = onToggleMute,
                    onPreviousStop = onPreviousStop,
                    onNextStop = onNextStop,
                    onContinueRoute = onNextStop,
                    onRecenter = onRecenter,
                )
            }
        }
    }
}

@Composable
private fun PermissionBanner(
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .padding(14.dp),
    ) {
        Text(
            text = stringResource(R.string.permission_title),
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.permission_body),
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
        Spacer(Modifier.height(10.dp))
        Row {
            BannerButton(
                label = stringResource(R.string.permission_allow),
                onClick = onAllow,
            )
            Spacer(Modifier.width(8.dp))
            BannerButton(
                label = stringResource(R.string.permission_settings),
                onClick = onOpenSettings,
                filled = false,
            )
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            color = TextPrimary,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        BannerButton(
            label = stringResource(R.string.action_retry),
            onClick = onRetry,
        )
    }
}

@Composable
private fun BannerButton(
    label: String,
    onClick: () -> Unit,
    filled: Boolean = true,
) {
    Text(
        text = label,
        color = if (filled) TextOnForest else ForestGreen,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (filled) ForestGreen else Color(0x14163833))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

// region Preview

@Preview(
    name = "Карта",
    showBackground = true,
    showSystemUi = true,
    device = "spec:width=411dp,height=891dp"
)
@Composable
private fun LiveMapScreenPreview() {
    HomeworkTheme {
        LiveMapContent(state = previewMapState())
    }
}

@Preview(
    name = "Без геолокации",
    showBackground = true,
    showSystemUi = true,
    device = "spec:width=411dp,height=891dp"
)
@Composable
private fun LiveMapScreenPermissionPreview() {
    HomeworkTheme {
        LiveMapContent(
            state = previewMapState().copy(
                user = null,
                permissionGranted = false,
                routePanelExpanded = false,
                routePlaces = emptyList(),
                route = null,
            ),
        )
    }
}

private fun previewMapState(): LiveMapUiState {
    val kremlin = OsmPlace(
        id = "kremlin",
        name = "Казанский Кремль",
        lat = 55.7986,
        lon = 49.1064,
        category = PlaceCategory.Historic,
        description = "Исторический комплекс.",
    )
    val mosque = OsmPlace(
        id = "kul-sharif",
        name = "Кул-Шариф",
        lat = 55.7984,
        lon = 49.1051,
        category = PlaceCategory.Mosque,
        description = "Соборная мечеть.",
    )
    val cafe = OsmPlace(
        id = "bauman",
        name = "Чәй йорты",
        lat = 55.7895,
        lon = 49.1167,
        category = PlaceCategory.Cafe,
        description = "Татарская кухня на Баумана.",
    )
    val park = OsmPlace(
        id = "kaban",
        name = "оз. Кабан",
        lat = 55.7778,
        lon = 49.1220,
        category = PlaceCategory.Park,
        description = "Набережная.",
    )
    val user = GeoLocation(lat = KazanCenter.lat, lon = KazanCenter.lon, accuracyMeters = 18f)
    return LiveMapUiState(
        user = user,
        places = listOf(kremlin, mosque, cafe, park),
        permissionGranted = true,
        routePlaces = listOf(
            RoutePlace(kremlin, order = 1, visitDurationMinutes = 25),
            RoutePlace(mosque, order = 2, visitDurationMinutes = 15),
        ),
        transportMode = TransportMode.Walking,
        route = RouteResult(
            geometry = listOf(
                user,
                GeoLocation(kremlin.lat, kremlin.lon),
                GeoLocation(mosque.lat, mosque.lon),
            ),
            distanceMeters = 1_240.0,
            durationSeconds = 16 * 60.0,
            steps = emptyList(),
            mode = TransportMode.Walking,
        ),
        routePanelExpanded = true,
    )
}

// endregion
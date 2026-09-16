package com.example.homework.ui.feature.map

import android.Manifest
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.homework.ui.uikit.theme.ForestGreen
import com.example.homework.ui.uikit.theme.TextOnForest
import com.example.homework.ui.uikit.theme.TextPrimary
import com.example.homework.ui.uikit.theme.TextSecondary
import com.example.homework.ui.uikit.component.RecenterChip
import com.example.homework.ui.uikit.component.StatusChip
import org.koin.androidx.compose.koinViewModel

@Composable
fun LiveMapScreen(
    modifier: Modifier = Modifier,
    viewModel: LiveMapViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
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

    fun openYandex() {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(state.yandexUrl)))
    }

    fun shareRoute() {
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, state.yandexUrl)
                },
                "Поделиться маршрутом",
            ),
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        OsmMap(
            center = state.mapCenter,
            user = state.user,
            places = state.filteredPlaces,
            routePlaces = state.routePlaces,
            selectedPlaceId = state.selectedPlaceId,
            routeGeometry = state.route?.geometry,
            routeDirty = state.routeDirty,
            followUser = state.followUser,
            recenterToken = state.recenterToken,
            fitRouteToken = state.fitRouteToken,
            onPlaceSelected = viewModel::selectPlace,
            onUserMapInteraction = viewModel::onUserMapInteraction,
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatusChip(
                title = if (state.user != null) "Вы здесь" else "Казань",
                subtitle = when {
                    state.isLoadingPlaces -> "Загружаем места рядом…"
                    state.filteredPlaces.isNotEmpty() -> "${state.filteredPlaces.size} мест рядом"
                    else -> "Места появятся после ответа Overpass"
                },
                loading = state.isLoadingPlaces,
            )
            if (!state.navigation.isActive) {
                PlaceFilterBar(
                    places = state.places,
                    selected = state.placeFilter,
                    onSelect = viewModel::setPlaceFilter,
                )
            }
            if (!state.permissionGranted) {
                PermissionBanner(
                    onAllow = {
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
                )
            }
            state.errorMessage?.let { message ->
                ErrorBanner(message = message, onRetry = viewModel::retryPlaces)
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
            RecenterChip(onClick = viewModel::recenter)
            if (state.navigation.isActive) {
                NavigationHud(
                    navigation = state.navigation,
                    currentPlaceName = state.routePlaces.getOrNull(state.navigation.placeIndex)?.name,
                    onPauseResume = viewModel::pauseOrResumeNavigation,
                    onRecenter = viewModel::recenter,
                    onRetry = viewModel::retryNavigation,
                    onNext = viewModel::nextNavigationPlace,
                    onExit = viewModel::stopNavigation,
                )
            } else if (state.selectedPlace == null) {
                RouteBuilderPanel(
                    routePlaces = state.routePlaces,
                    transportMode = state.transportMode,
                    route = state.route,
                    routeDirty = state.routeDirty,
                    isBuilding = state.isBuildingRoute,
                    expanded = state.routePanelExpanded,
                    onToggleExpanded = viewModel::toggleRoutePanel,
                    onRemove = viewModel::removeFromRoute,
                    onDurationDelta = viewModel::changeVisitDuration,
                    onTransportMode = viewModel::setTransportMode,
                    onBuild = viewModel::buildRoute,
                    onOptimize = viewModel::optimizeAndBuild,
                    onOpenYandex = { openYandex() },
                    onShare = { shareRoute() },
                    onStartNavigation = viewModel::startNavigation,
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
                    onDismiss = { viewModel.selectPlace(null) },
                    onToggleRoute = viewModel::toggleSelectedInRoute,
                    onOpenGuide = viewModel::openGuide,
                    onCloseGuide = viewModel::closeGuide,
                    onMarkVisited = viewModel::markPlaceVisited,
                    onTogglePlayback = viewModel::toggleGuidePlayback,
                    onSeek = viewModel::seekGuide,
                    onToggleMute = viewModel::toggleGuideSound,
                    onPreviousStop = viewModel::goToPreviousStop,
                    onNextStop = viewModel::goToNextStop,
                    onContinueRoute = viewModel::goToNextStop,
                    onRecenter = viewModel::recenter,
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
            text = "Нужен доступ к геолокации",
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Так карта покажет, где вы сейчас, и подгрузит места вокруг.",
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
        Spacer(Modifier.height(10.dp))
        Row {
            BannerButton("Разрешить", onAllow)
            Spacer(Modifier.width(8.dp))
            BannerButton("Настройки", onOpenSettings, filled = false)
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
        BannerButton("Повторить", onRetry)
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

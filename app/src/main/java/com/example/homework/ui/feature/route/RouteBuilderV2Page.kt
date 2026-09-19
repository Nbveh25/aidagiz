package com.example.homework.ui.feature.route

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.homework.R
import com.example.homework.core.locale.AppLanguage
import com.example.homework.entity.tour.BuilderView
import com.example.homework.entity.tour.WalkPace
import com.example.homework.ui.feature.map.LiveMapViewModel
import com.example.homework.ui.feature.map.NavigationHud
import com.example.homework.ui.feature.map.OsmMap
import com.example.homework.ui.feature.map.PlaceDetailsBottomSheet
import com.example.homework.ui.uikit.component.LanguageToggle
import com.example.homework.ui.uikit.component.RecenterChip
import com.example.homework.ui.uikit.theme.Cream
import com.example.homework.ui.uikit.theme.ForestGreen
import com.example.homework.ui.uikit.theme.SerifFamily
import com.example.homework.ui.uikit.theme.TextOnForest
import org.koin.androidx.compose.koinViewModel

@Composable
fun RouteBuilderV2Page(
    language: AppLanguage,
    onLanguageSelect: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LiveMapViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
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

    AnimatedContent(
        targetState = state.builderView,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        modifier = modifier.fillMaxSize(),
        label = "builder-view",
    ) { view ->
        if (view == BuilderView.Form) {
            RouteBuilderFormContent(
                language = language,
                onLanguageSelect = onLanguageSelect,
                onDuration = viewModel::setDurationMinutes,
                onToggleInterest = viewModel::toggleInterest,
                onPace = viewModel::setPace,
                onAiRequest = viewModel::setAiRequest,
                onSubmit = viewModel::submitAdventureRoute,
                formValid = state.formState.isValid,
                form = state.formState,
            )
        } else {
            RoutePlannerContent(
                state = state,
                language = language,
                onLanguageSelect = onLanguageSelect,
                onPlaceSelected = viewModel::selectPlace,
                onUserMapInteraction = viewModel::onUserMapInteraction,
                onRecenter = viewModel::recenter,
                onToggleParams = viewModel::toggleParamsExpanded,
                onDuration = viewModel::setDurationMinutes,
                onToggleInterest = viewModel::toggleInterest,
                onPace = viewModel::setPace,
                onAiRequest = viewModel::setAiRequest,
                onSubmit = viewModel::submitAdventureRoute,
                onRetry = viewModel::retryAdventureRoute,
                onRebuildPrompt = viewModel::setRebuildPrompt,
                onRebuild = viewModel::rebuildAdventure,
                onStartNavigation = viewModel::startNavigation,
                onPauseResumeNavigation = viewModel::pauseOrResumeNavigation,
                onRetryNavigation = viewModel::retryNavigation,
                onNextNavigationPlace = viewModel::nextNavigationPlace,
                onStopNavigation = viewModel::stopNavigation,
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
            )
        }
    }
}

@Composable
private fun RouteBuilderFormContent(
    language: AppLanguage,
    onLanguageSelect: (AppLanguage) -> Unit,
    onDuration: (Int) -> Unit,
    onToggleInterest: (String) -> Unit,
    onPace: (WalkPace) -> Unit,
    onAiRequest: (String) -> Unit,
    onSubmit: () -> Unit,
    formValid: Boolean,
    form: com.example.homework.entity.tour.RouteFormState,
) {
    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.map_kazan),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to Cream.copy(alpha = 0.97f),
                        0.62f to Cream.copy(alpha = 0.88f),
                        1f to Color.Transparent,
                    ),
                ),
        )
        Column(Modifier.fillMaxSize()) {
            LanguageToggle(
                selected = language,
                onSelect = onLanguageSelect,
                modifier = Modifier
                    .align(Alignment.End)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Text(
                    text = stringResource(R.string.builder_title_1),
                    color = ForestGreen,
                    fontFamily = SerifFamily,
                    fontSize = 36.sp,
                    lineHeight = 40.sp,
                )
                Text(
                    text = stringResource(R.string.builder_title_2),
                    color = ForestGreen,
                    fontFamily = SerifFamily,
                    fontSize = 36.sp,
                    lineHeight = 40.sp,
                )
                Spacer(Modifier.height(20.dp))
                RouteFormFields(
                    form = form,
                    onDuration = onDuration,
                    onToggleInterest = onToggleInterest,
                    onPace = onPace,
                    onAiRequest = onAiRequest,
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.builder_next),
                    color = TextOnForest,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (formValid) ForestGreen else ForestGreen.copy(alpha = 0.4f))
                        .clickable(enabled = formValid, onClick = onSubmit)
                        .padding(vertical = 16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun RoutePlannerContent(
    state: com.example.homework.ui.feature.map.state.LiveMapUiState,
    language: AppLanguage,
    onLanguageSelect: (AppLanguage) -> Unit,
    onPlaceSelected: (String?) -> Unit,
    onUserMapInteraction: () -> Unit,
    onRecenter: () -> Unit,
    onToggleParams: () -> Unit,
    onDuration: (Int) -> Unit,
    onToggleInterest: (String) -> Unit,
    onPace: (WalkPace) -> Unit,
    onAiRequest: (String) -> Unit,
    onSubmit: () -> Unit,
    onRetry: () -> Unit,
    onRebuildPrompt: (String) -> Unit,
    onRebuild: () -> Unit,
    onStartNavigation: () -> Unit,
    onPauseResumeNavigation: () -> Unit,
    onRetryNavigation: () -> Unit,
    onNextNavigationPlace: () -> Unit,
    onStopNavigation: () -> Unit,
    onDismissPlace: () -> Unit,
    onToggleSelectedInRoute: () -> Unit,
    onOpenGuide: () -> Unit,
    onCloseGuide: () -> Unit,
    onMarkVisited: () -> Unit,
    onTogglePlayback: () -> Unit,
    onSeekGuide: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onPreviousStop: () -> Unit,
    onNextStop: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream),
    ) {
        OsmMap(
                center = state.mapCenter,
                user = state.user,
                places = state.routePlaces.map { it.place }.ifEmpty { state.filteredPlaces },
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
        LanguageToggle(
            selected = language,
            onSelect = onLanguageSelect,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(),
        ) {
                RecenterChip(
                    onClick = onRecenter,
                    modifier = Modifier.padding(start = 16.dp, bottom = 12.dp),
                )
                if (state.navigation.isActive) {
                    NavigationHud(
                        navigation = state.navigation,
                        currentPlaceName = state.routePlaces.getOrNull(state.navigation.placeIndex)?.name,
                        onPauseResume = onPauseResumeNavigation,
                        onRecenter = onRecenter,
                        onRetry = onRetryNavigation,
                        onNext = onNextNavigationPlace,
                        onExit = onStopNavigation,
                        modifier = Modifier.padding(16.dp),
                    )
                } else if (state.selectedPlace == null) {
                    AdventurePlannerPanel(
                        form = state.formState,
                        routeStatus = state.routeStatus,
                        adventure = state.adventure,
                        routePlaces = state.routePlaces,
                        paramsExpanded = state.paramsExpanded,
                        rebuildPrompt = state.rebuildPrompt,
                        errorMessage = state.errorMessage,
                        canStartNavigation = state.routePlaces.isNotEmpty(),
                        onToggleParams = onToggleParams,
                        onDuration = onDuration,
                        onToggleInterest = onToggleInterest,
                        onPace = onPace,
                        onAiRequest = onAiRequest,
                        onSubmit = onSubmit,
                        onRetry = onRetry,
                        onRebuildPrompt = onRebuildPrompt,
                        onRebuild = onRebuild,
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

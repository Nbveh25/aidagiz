package com.example.homework.ui.feature.map

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.homework.R
import com.example.homework.entity.guide.AiGuideNarration
import com.example.homework.entity.guide.AiGuidePlayback
import com.example.homework.entity.map.OsmPlace
import com.example.homework.entity.place.PlaceDetails
import com.example.homework.entity.tour.TourProgress
import com.example.homework.ui.locale.labelRes
import com.example.homework.ui.locale.localizedDistance
import com.example.homework.ui.uikit.theme.ForestGreen
import com.example.homework.ui.uikit.theme.ForestGreenDeep
import com.example.homework.ui.uikit.theme.SheetWhite
import com.example.homework.ui.uikit.theme.TextOnForest
import com.example.homework.ui.uikit.theme.TextPrimary
import com.example.homework.ui.uikit.theme.TextSecondary

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceDetailsBottomSheet(
    place: OsmPlace,
    details: PlaceDetails?,
    narration: AiGuideNarration?,
    playback: AiGuidePlayback,
    tourProgress: TourProgress,
    distanceMeters: Int?,
    isGuideOpen: Boolean,
    inRoute: Boolean,
    isLoadingDetails: Boolean = false,
    onDismiss: () -> Unit,
    onToggleRoute: () -> Unit,
    onOpenGuide: () -> Unit,
    onCloseGuide: () -> Unit,
    onMarkVisited: () -> Unit,
    onTogglePlayback: () -> Unit,
    onSeek: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onPreviousStop: () -> Unit,
    onNextStop: () -> Unit,
    onContinueRoute: () -> Unit,
    onRecenter: () -> Unit,
) {
    var detailsExpanded by remember(place.id) { mutableStateOf(false) }
    val sheetSize = when {
        isGuideOpen -> PlaceSheetSize.Guide
        detailsExpanded -> PlaceSheetSize.Details
        else -> PlaceSheetSize.Peek
    }
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    val targetHeight = when (sheetSize) {
        PlaceSheetSize.Peek -> screenHeight / 3
        PlaceSheetSize.Details -> screenHeight
        PlaceSheetSize.Guide -> screenHeight
    }

    val sheetHeight by animateDpAsState(
        targetValue = targetHeight,
        label = "placeSheetHeight",
    )
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val meta = buildList {
        add(stringResource(place.category.labelRes))
        if (distanceMeters != null) add(localizedDistance(distanceMeters))
        details?.firstMentionYear?.let { year ->
            add(stringResource(R.string.historical_first_mention, year))
        }
        (details?.openingHours ?: place.openingHours)?.let { add(it) }
    }.joinToString(" · ")

    ModalBottomSheet(
        sheetState = sheetState,
        containerColor = if (sheetSize == PlaceSheetSize.Guide) Color.Transparent else SheetWhite,
        shape = if (sheetSize == PlaceSheetSize.Guide) {
            RectangleShape
        } else {
            RoundedCornerShape(
                topStart = 28.dp,
                topEnd = 28.dp,
            )
        },
        dragHandle = null,
        onDismissRequest = onDismiss,
    ) {
        if (sheetSize == PlaceSheetSize.Guide) {
            AudioGuideContent(
                place = place,
                details = details,
                narration = narration,
                playback = playback,
                tourProgress = tourProgress,
                onClose = onDismiss,
                onTogglePlayback = onTogglePlayback,
                onSeek = onSeek,
                onToggleMute = onToggleMute,
                onPreviousStop = onPreviousStop,
                onNextStop = onNextStop,
                onContinueRoute = onContinueRoute,
                onRecenter = onRecenter,
                onBack = {
                    detailsExpanded = true
                    onCloseGuide()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(sheetHeight),
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(sheetHeight)
                    .navigationBarsPadding()
                    .padding(
                        start = 20.dp,
                        end = 20.dp,
                        top = 10.dp,
                        bottom = 16.dp,
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 12.dp)
                        .size(
                            width = 36.dp,
                            height = 4.dp,
                        )
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFD5D3CE)),
                )
                if (sheetSize == PlaceSheetSize.Details) {
                    ExpandedPlaceContent(
                        place = place,
                        details = details,
                        meta = meta,
                        isLoadingDetails = isLoadingDetails,
                        onListen = onOpenGuide,
                        onToggleRoute = onToggleRoute,
                        inRoute = inRoute,
                        onMarkVisited = onMarkVisited,
                        modifier = Modifier
                            .weight(1f),
                        onCollapse = {
                            detailsExpanded = false
                        },
                    )
                } else {
                    CollapsedPlaceContent(
                        place = place,
                        details = details,
                        meta = meta,
                        inRoute = inRoute,
                        isLoadingDetails = isLoadingDetails,
                        onToggleRoute = onToggleRoute,
                        onExpand = {
                            detailsExpanded = true
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private enum class PlaceSheetSize {
    Peek,
    Details,
    Guide,
}

@Composable
private fun CollapsedPlaceContent(
    place: OsmPlace,
    details: PlaceDetails?,
    meta: String,
    inRoute: Boolean,
    isLoadingDetails: Boolean,
    onToggleRoute: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlacePhoto(
                imageUrl = details?.imageUrl ?: place.imageUrl,
                contentDescription = place.name,
                loading = isLoadingDetails,
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(16.dp)),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                if (isLoadingDetails) {
                    HistoricalDetailsLoader()
                } else {
                    Text(
                        text = details?.name ?: place.name,
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 22.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = details?.shortDescription.orEmpty(),
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.place_more),
                tint = TextSecondary,
                modifier = Modifier
                    .size(28.dp)
                    .clickable(onClick = onExpand),
            )
        }
        if (!isLoadingDetails) {
            Spacer(Modifier.height(14.dp))
            Text(
                text = meta,
                color = TextSecondary,
                fontSize = 13.sp,
            )
        }
        Spacer(Modifier.weight(1f))
        PlaceSheetButton(
            label = if (inRoute) {
                stringResource(R.string.place_remove_from_route)
            } else {
                stringResource(R.string.place_add_to_route)
            },
            filled = !inRoute,
            onClick = onToggleRoute,
        )
    }
}

@Composable
private fun ExpandedPlaceContent(
    place: OsmPlace,
    details: PlaceDetails?,
    meta: String,
    isLoadingDetails: Boolean,
    onCollapse: () -> Unit,
    onListen: () -> Unit,
    onToggleRoute: () -> Unit,
    inRoute: Boolean,
    onMarkVisited: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        PlacePhoto(
            imageUrl = details?.imageUrl ?: place.imageUrl,
            contentDescription = place.name,
            loading = isLoadingDetails,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(18.dp)),
        )
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f)) {
                if (isLoadingDetails) {
                    HistoricalDetailsLoader()
                } else {
                    Text(
                        text = details?.name ?: place.name,
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 22.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = meta,
                        color = TextSecondary,
                        fontSize = 13.sp,
                    )
                }
            }
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(R.string.place_collapse),
                tint = TextSecondary,
                modifier = Modifier
                    .size(28.dp)
                    .clickable(onClick = onCollapse),
            )
        }
        Spacer(Modifier.height(16.dp))
        if (isLoadingDetails) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                HistoricalDetailsLoader()
            }
        } else {
            Text(
                text = stringResource(R.string.place_ai_guide),
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = details?.fullDescription.orEmpty(),
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            )
        }
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PlaceSheetButton(
                label = if (inRoute) {
                    stringResource(R.string.place_remove_from_route)
                } else {
                    stringResource(R.string.place_add_to_route)
                },
                filled = !inRoute,
                onClick = onToggleRoute,
            )
            PlaceSheetButton(
                label = stringResource(R.string.place_listen_guide),
                filled = true,
                onClick = onListen,
            )
            PlaceSheetButton(
                label = stringResource(R.string.place_mark_visited),
                filled = false,
                onClick = onMarkVisited,
            )
        }
    }
}

@Composable
private fun PlaceSheetButton(
    label: String,
    filled: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        color = if (filled) TextOnForest else ForestGreenDeep,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (filled) ForestGreenDeep else Color(0x14163833))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun PlacePhoto(
    imageUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
) {
    Box(
        modifier = modifier.background(Color(0xFFE7E4DC)),
        contentAlignment = Alignment.Center,
    ) {
        when {
            loading && imageUrl.isNullOrBlank() -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = ForestGreen,
                    strokeWidth = 2.dp,
                )
            }
            imageUrl.isNullOrBlank() -> {
                Text(
                    text = stringResource(R.string.place_no_photo),
                    color = TextSecondary,
                    fontSize = 12.sp,
                )
            }
            else -> {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun HistoricalDetailsLoader(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            color = ForestGreen,
            strokeWidth = 2.dp,
        )
        Text(
            text = stringResource(R.string.historical_details_loading),
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
    }
}

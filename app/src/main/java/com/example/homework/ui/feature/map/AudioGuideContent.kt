package com.example.homework.ui.feature.map

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homework.R
import com.example.homework.entity.guide.AiGuideNarration
import com.example.homework.entity.guide.AiGuidePlayback
import com.example.homework.entity.map.OsmPlace
import com.example.homework.entity.place.PlaceDetails
import com.example.homework.entity.tour.TourProgress
import com.example.homework.ui.uikit.theme.ForestGreenDeep
import com.example.homework.ui.uikit.theme.IconDark
import com.example.homework.ui.uikit.theme.ProgressTrack
import com.example.homework.ui.uikit.theme.SheetWhite
import com.example.homework.ui.uikit.theme.TextOnForest
import com.example.homework.ui.uikit.theme.TextPrimary
import com.example.homework.ui.uikit.theme.TextSecondary
import com.example.homework.ui.uikit.theme.UserBlue

@Composable
fun AudioGuideContent(
    place: OsmPlace,
    details: PlaceDetails?,
    narration: AiGuideNarration?,
    playback: AiGuidePlayback,
    tourProgress: TourProgress,
    isPreparingAudio: Boolean = false,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onTogglePlayback: () -> Unit,
    onSeek: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onPreviousStop: () -> Unit,
    onNextStop: () -> Unit,
    onContinueRoute: () -> Unit,
    onRecenter: () -> Unit,
) {
    var showFullText by remember(place.id) { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(
                    end = 16.dp,
                    top = 12.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MapRoundIconButton(
                icon = if (playback.isMuted) {
                    Icons.AutoMirrored.Filled.VolumeOff
                } else {
                    Icons.AutoMirrored.Filled.VolumeUp
                },
                contentDescription = if (playback.isMuted) {
                    stringResource(R.string.guide_unmute)
                } else {
                    stringResource(R.string.guide_mute)
                },
                onClick = onToggleMute,
            )
            MapRoundIconButton(
                icon = Icons.Outlined.NearMe,
                contentDescription = stringResource(R.string.guide_navigation),
                onClick = onRecenter,
            )
        }
        Card(
            shape = RoundedCornerShape(
                topStart = 28.dp,
                topEnd = 28.dp,
            ),
            colors = CardDefaults.cardColors(containerColor = SheetWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(
                        start = 20.dp,
                        end = 20.dp,
                        top = 10.dp,
                        bottom = 88.dp,
                    ),
            ) {
                GuideProgressRow(
                    current = tourProgress.currentStep,
                    total = tourProgress.totalSteps,
                    onBack = onBack,
                    onClose = onClose,
                )
                Spacer(Modifier.height(16.dp))
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        text = details?.name ?: place.name,
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 26.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (details?.tags ?: emptyList()).forEach { tag ->
                            GuideChip(tag)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            tint = ForestGreenDeep,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = details?.address.orEmpty(),
                            color = TextSecondary,
                            fontSize = 13.sp,
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = stringResource(
                            if (isPreparingAudio) R.string.guide_preparing else R.string.guide_speaking,
                        ),
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(12.dp))
                    AudioPlayerRow(
                        isPlaying = playback.isPlaying,
                        isPreparing = isPreparingAudio,
                        onTogglePlayback = onTogglePlayback,
                        onPrevious = onPreviousStop,
                        onNext = onNextStop,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = playback.positionLabel,
                            color = TextSecondary,
                            fontSize = 12.sp,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = playback.remainingLabel,
                            color = TextSecondary,
                            fontSize = 12.sp,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = narration?.text.orEmpty(),
                        color = TextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        maxLines = if (showFullText) Int.MAX_VALUE else 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.clickable { showFullText = !showFullText },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (showFullText) {
                                stringResource(R.string.guide_hide_text)
                            } else {
                                stringResource(R.string.guide_show_text)
                            },
                            color = ForestGreenDeep,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            tint = ForestGreenDeep,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            containerColor = ForestGreenDeep,
            contentColor = TextOnForest,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Route,
                    contentDescription = null,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.guide_continue),
                    fontWeight = FontWeight.Medium,
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            onClick = onContinueRoute,
        )
    }
}

@Composable
private fun GuideProgressRow(
    current: Int,
    total: Int,
    onBack: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            modifier = Modifier.size(36.dp),
            onClick = onBack,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.guide_back),
                tint = IconDark,
            )
        }
        Text(
            text = stringResource(R.string.guide_progress, current, total),
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.width(10.dp))
        Spacer(Modifier.weight(1f))
        IconButton(
            modifier = Modifier.size(36.dp),
            onClick = onClose,
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.guide_close),
                tint = IconDark,
            )
        }
    }
}

@Composable
private fun GuideChip(label: String) {
    Text(
        text = label,
        color = ForestGreenDeep,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x1A0B3A2E))
            .padding(
                horizontal = 10.dp,
                vertical = 6.dp,
            ),
    )
}

@Composable
private fun AudioPlayerRow(
    isPlaying: Boolean,
    isPreparing: Boolean,
    onTogglePlayback: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        SoundWaveStub(
            modifier = Modifier
                .weight(1f)
                .height(36.dp)
                .padding(horizontal = 8.dp),
            played = isPlaying,
        )
        Box(
            modifier = Modifier
                .size(56.dp)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(ForestGreenDeep)
                .clickable(enabled = !isPreparing, onClick = onTogglePlayback),
            contentAlignment = Alignment.Center,
        ) {
            if (isPreparing) {
                CircularProgressIndicator(
                    color = TextOnForest,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(26.dp),
                )
            } else {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) {
                        stringResource(R.string.guide_pause)
                    } else {
                        stringResource(R.string.guide_play)
                    },
                    tint = TextOnForest,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        SoundWaveStub(
            modifier = Modifier
                .weight(1f)
                .height(36.dp)
                .padding(horizontal = 8.dp),
            played = false,
        )
    }
}

@Composable
private fun SoundWaveStub(
    modifier: Modifier = Modifier,
    played: Boolean = true,
) {
    val barColor = if (played) ForestGreenDeep else ProgressTrack
    val heights = remember {
        floatArrayOf(0.35f, 0.7f, 0.45f, 0.9f, 0.55f, 0.8f, 0.4f, 0.65f, 0.5f, 0.75f, 0.38f, 0.6f)
    }
    Canvas(modifier) {
        val barWidth = size.width / (heights.size * 2f)
        val gap = barWidth
        heights.forEachIndexed { index, fraction ->
            val h = size.height * fraction
            val x = index * (barWidth + gap) + barWidth / 2f
            drawLine(
                color = barColor,
                start = Offset(x, (size.height - h) / 2f),
                end = Offset(x, (size.height + h) / 2f),
                strokeWidth = barWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun MapRoundIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .background(Color.White),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = IconDark,
            modifier = Modifier.size(22.dp),
        )
    }
}

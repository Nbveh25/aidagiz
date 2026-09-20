package com.example.homework.ui.feature.route

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homework.R
import com.example.homework.entity.map.RoutePlace
import com.example.homework.entity.tour.AdventureRoute
import com.example.homework.entity.tour.KazanTime
import com.example.homework.entity.tour.RouteFormState
import com.example.homework.entity.tour.RouteStatus
import com.example.homework.entity.tour.WalkPace
import com.example.homework.ui.locale.labelRes
import com.example.homework.ui.uikit.theme.Cream
import com.example.homework.ui.uikit.theme.CreamDeep
import com.example.homework.ui.uikit.theme.ForestGreen
import com.example.homework.ui.uikit.theme.GoldAccent
import com.example.homework.ui.uikit.theme.SerifFamily
import com.example.homework.ui.uikit.theme.TextOnForest
import com.example.homework.ui.uikit.theme.TextPrimary
import com.example.homework.ui.uikit.theme.TextSecondary

@Composable
fun AdventurePlannerPanel(
    form: RouteFormState,
    routeStatus: RouteStatus,
    adventure: AdventureRoute?,
    routePlaces: List<RoutePlace>,
    paramsExpanded: Boolean,
    rebuildPrompt: String,
    errorMessage: String?,
    canStartNavigation: Boolean,
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
    customStart: com.example.homework.entity.map.GeoLocation?,
    onUseMyLocation: () -> Unit,
    onPickStartOnMap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(Cream)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(36.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(GoldAccent.copy(alpha = 0.45f)),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.builder_preview_1),
            color = ForestGreen,
            fontFamily = SerifFamily,
            fontSize = 28.sp,
            lineHeight = 32.sp,
        )
        Text(
            text = stringResource(R.string.builder_preview_2),
            color = ForestGreen,
            fontFamily = SerifFamily,
            fontSize = 28.sp,
            lineHeight = 32.sp,
        )
        Spacer(Modifier.height(10.dp))
        val stats = adventure?.let {
            stringResource(
                R.string.builder_stats,
                it.totalDurationMinutes,
                it.totalTravelDurationMinutes,
                it.totalVisitDurationMinutes,
                it.places.size,
            )
        }
        if (stats != null && routeStatus == RouteStatus.Success) {
            Text(text = stats, color = TextSecondary, fontSize = 13.sp)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 320.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            if (routeStatus == RouteStatus.Loading) {
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = ForestGreen,
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.builder_picking),
                        color = TextPrimary,
                        fontSize = 15.sp,
                    )
                }
            }

            if (routeStatus == RouteStatus.Error) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = errorMessage ?: stringResource(R.string.error_build_route),
                    color = TextPrimary,
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(10.dp))
                PlannerButton(
                    label = stringResource(R.string.builder_retry),
                    onClick = onRetry,
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.builder_params),
                        color = ForestGreen,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                    Text(
                        text = stringResource(
                            R.string.builder_params_summary,
                            form.durationMinutes,
                            stringResource(form.pace.labelRes),
                            form.interests.size,
                        ),
                        color = TextSecondary,
                        fontSize = 12.sp,
                    )
                }
                Text(
                    text = stringResource(R.string.builder_edit),
                    color = ForestGreen,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable(onClick = onToggleParams),
                )
            }
            if (paramsExpanded) {
                Spacer(Modifier.height(12.dp))
                RouteFormFields(
                    form = form,
                    onDuration = onDuration,
                    onToggleInterest = onToggleInterest,
                    onPace = onPace,
                    onAiRequest = onAiRequest,
                    compact = true,
                    customStart = customStart,
                    onUseMyLocation = onUseMyLocation,
                    onPickOnMap = onPickStartOnMap,
                )
                Spacer(Modifier.height(12.dp))
                PlannerButton(
                    label = stringResource(R.string.builder_next),
                    onClick = onSubmit,
                    enabled = form.isValid && routeStatus != RouteStatus.Loading,
                )
            }

            if (routeStatus != RouteStatus.Loading) {
                Spacer(Modifier.height(18.dp))
                if (routePlaces.isEmpty()) {
                    Text(
                        text = stringResource(R.string.builder_timeline_empty),
                        color = TextSecondary,
                        fontSize = 14.sp,
                    )
                } else {
                    routePlaces.forEach { item ->
                        TimelineStop(item)
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }

            if (routeStatus == RouteStatus.Success) {
                WishField(
                    value = rebuildPrompt,
                    onValueChange = onRebuildPrompt,
                    placeholder = stringResource(R.string.builder_rebuild_placeholder),
                )
                Spacer(Modifier.height(10.dp))
                PlannerButton(
                    label = stringResource(R.string.builder_rebuild),
                    onClick = onRebuild,
                    filled = false,
                    enabled = rebuildPrompt.isNotBlank() && routeStatus != RouteStatus.Loading,
                )
            }
        }

        if (canStartNavigation && routeStatus == RouteStatus.Success) {
            Spacer(Modifier.height(12.dp))
            PlannerButton(
                label = stringResource(R.string.builder_start_nav),
                onClick = onStartNavigation,
            )
        }
    }
}

@Composable
private fun TimelineStop(item: RoutePlace) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "${item.order}.",
            color = GoldAccent,
            fontFamily = SerifFamily,
            fontSize = 16.sp,
            modifier = Modifier.width(28.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = item.name,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val arrival = KazanTime.formatClock(item.arrivalAt)
            val departure = KazanTime.formatClock(item.departureAt)
            val timeRange = if (arrival != null && departure != null) "$arrival–$departure · " else ""
            Text(
                text = timeRange + stringResource(
                    R.string.builder_stop_meta,
                    stringResource(item.category.labelRes),
                    item.visitDurationMinutes,
                ),
                color = TextSecondary,
                fontSize = 12.sp,
            )
            if (item.travelDurationMinutes > 0) {
                Text(
                    text = stringResource(R.string.builder_stop_travel, item.travelDurationMinutes),
                    color = TextSecondary,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun PlannerButton(
    label: String,
    onClick: () -> Unit,
    filled: Boolean = true,
    enabled: Boolean = true,
) {
    Text(
        text = label,
        color = when {
            !enabled -> TextSecondary
            filled -> TextOnForest
            else -> ForestGreen
        },
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                when {
                    !enabled -> CreamDeep
                    filled -> ForestGreen
                    else -> CreamDeep
                },
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

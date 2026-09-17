package com.example.homework.ui.feature.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homework.R
import com.example.homework.entity.map.RoutePlace
import com.example.homework.entity.map.RouteResult
import com.example.homework.entity.map.TransportMode
import com.example.homework.ui.locale.labelRes
import com.example.homework.ui.locale.localizedDistance
import com.example.homework.ui.uikit.theme.ForestGreen
import com.example.homework.ui.uikit.theme.TextOnForest
import com.example.homework.ui.uikit.theme.TextPrimary
import com.example.homework.ui.uikit.theme.TextSecondary

@Composable
fun RouteBuilderPanel(
    routePlaces: List<RoutePlace>,
    transportMode: TransportMode,
    route: RouteResult?,
    routeDirty: Boolean,
    isBuilding: Boolean,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onRemove: (String) -> Unit,
    onDurationDelta: (String, Int) -> Unit,
    onTransportMode: (TransportMode) -> Unit,
    onBuild: () -> Unit,
    onOptimize: () -> Unit,
    onOpenYandex: () -> Unit,
    onShare: () -> Unit,
    onStartNavigation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpanded),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (routePlaces.isEmpty()) {
                        stringResource(R.string.route_title)
                    } else {
                        stringResource(
                            R.string.route_title_count,
                            routePlaces.size,
                        )
                    },
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                val subtitle = when {
                    isBuilding -> stringResource(R.string.route_building)
                    routeDirty && route != null -> stringResource(R.string.route_dirty)
                    route != null -> stringResource(
                        R.string.route_summary,
                        localizedDistance(route.distanceMeters.toInt()),
                        (route.durationSeconds / 60).toInt(),
                    )

                    routePlaces.size >= 2 -> stringResource(R.string.route_ready)
                    else -> stringResource(R.string.route_add_places)
                }
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 12.sp,
                )
            }
            Text(
                text = if (expanded) {
                    stringResource(R.string.route_hide)
                } else {
                    stringResource(R.string.route_show)
                },
                color = ForestGreen,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        if (!expanded) return
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TransportMode.entries.forEach { mode ->
                val active = mode == transportMode
                Text(
                    text = mode.let { stringResource(it.labelRes) },
                    color = if (active) TextOnForest else TextPrimary,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (active) ForestGreen else Color(0x14163833))
                        .clickable { onTransportMode(mode) }
                        .padding(
                            horizontal = 10.dp,
                            vertical = 6.dp,
                        ),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 180.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (routePlaces.isEmpty()) {
                Text(
                    text = stringResource(R.string.route_empty),
                    color = TextSecondary,
                    fontSize = 13.sp,
                )
            } else {
                routePlaces.forEach { item ->
                    RoutePlaceRow(
                        item = item,
                        onRemove = { onRemove(item.id) },
                        onMinus = { onDurationDelta(item.id, -5) },
                        onPlus = { onDurationDelta(item.id, 5) },
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PanelButton(
                label = stringResource(R.string.route_build),
                onClick = onBuild,
                enabled = routePlaces.isNotEmpty() && transportMode.isRoutable
            )
            PanelButton(
                label = stringResource(R.string.route_optimize),
                onClick = onOptimize,
                filled = false,
                enabled = routePlaces.size >= 2 && transportMode.isRoutable,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PanelButton(
                label = stringResource(R.string.route_yandex),
                onClick = onOpenYandex,
                filled = false,
                enabled = routePlaces.isNotEmpty(),
            )
            PanelButton(
                label = stringResource(R.string.route_share),
                onClick = onShare,
                filled = false,
                enabled = routePlaces.isNotEmpty(),
            )
        }
        if (transportMode == TransportMode.Walking) {
            Spacer(Modifier.height(8.dp))
            PanelButton(
                label = stringResource(R.string.route_start_nav),
                onClick = onStartNavigation,
                enabled = routePlaces.isNotEmpty(),
            )
        }
        if (transportMode == TransportMode.Transit) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.route_transit_hint),
                color = TextSecondary,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun RoutePlaceRow(
    item: RoutePlace,
    onRemove: () -> Unit,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${item.order}.",
            color = ForestGreen,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(24.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = item.name,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                text = stringResource(
                    R.string.route_stop_meta,
                    item.visitDurationMinutes,
                    stringResource(item.category.labelRes),
                ),
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
        Text(
            text = "−",
            modifier = Modifier
                .clickable(onClick = onMinus)
                .padding(6.dp), fontSize = 16.sp
        )
        Text(
            text = "+",
            modifier = Modifier
                .clickable(onClick = onPlus)
                .padding(6.dp), fontSize = 16.sp
        )
        Text(
            text = "✕",
            modifier = Modifier
                .clickable(onClick = onRemove)
                .padding(6.dp),
            color = TextSecondary
        )
    }
}

@Composable
private fun PanelButton(
    label: String,
    onClick: () -> Unit,
    filled: Boolean = true,
    enabled: Boolean = true,
) {
    Text(
        text = label,
        color = if (!enabled) TextSecondary else if (filled) TextOnForest else ForestGreen,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    !enabled -> Color(0xFFEFEFEA)
                    filled -> ForestGreen
                    else -> Color(0x14163833)
                },
            )
            .clickable(
                enabled = enabled,
                onClick = onClick,
            )
            .padding(
                horizontal = 12.dp,
                vertical = 8.dp,
            ),
    )
}

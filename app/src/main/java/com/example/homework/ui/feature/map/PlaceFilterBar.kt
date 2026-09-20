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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homework.R
import com.example.homework.entity.map.OsmPlace
import com.example.homework.entity.map.PlaceFilter
import com.example.homework.entity.map.YearRange
import com.example.homework.entity.map.placeFilterCount
import com.example.homework.ui.locale.labelRes
import com.example.homework.ui.uikit.theme.CreamDeep
import com.example.homework.ui.uikit.theme.ForestGreen
import com.example.homework.ui.uikit.theme.TextOnForest
import com.example.homework.ui.uikit.theme.TextPrimary
import com.example.homework.ui.uikit.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun PlaceFilterBar(
    places: List<OsmPlace>,
    selected: PlaceFilter,
    onSelect: (PlaceFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PlaceFilter.entries
            .filter { it != PlaceFilter.History }
            .forEach { filter ->
                val count = placeFilterCount(places, filter)
                val active = filter == selected
                Text(
                    text = "${stringResource(filter.labelRes)} $count",
                    color = if (active) TextOnForest else TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .shadow(4.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (active) ForestGreen else Color.White)
                        .clickable { onSelect(filter) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
    }
}

@Composable
fun HistoricalMapToggle(
    enabled: Boolean,
    yearRange: YearRange,
    onEnabledChange: (Boolean) -> Unit,
    onYearRangeChange: (YearRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    var slider by remember(yearRange.from, yearRange.to) {
        mutableStateOf(yearRange.from.toFloat()..yearRange.to.toFloat())
    }
    LaunchedEffect(yearRange.from, yearRange.to) {
        slider = yearRange.from.toFloat()..yearRange.to.toFloat()
    }
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .shadow(4.dp, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .padding(start = 10.dp, end = 2.dp, top = 1.dp, bottom = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.history_map_toggle),
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                modifier = Modifier.scale(0.68f),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = ForestGreen,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = CreamDeep,
                    uncheckedBorderColor = CreamDeep,
                ),
            )
        }
        if (enabled) {
            Spacer(Modifier.height(6.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(
                            R.string.history_map_years,
                            slider.start.roundToInt(),
                            slider.endInclusive.roundToInt(),
                        ),
                        color = ForestGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "${YearRange.MinYear}–${YearRange.MaxYear}",
                        color = TextSecondary,
                        fontSize = 10.sp,
                    )
                }
                RangeSlider(
                    value = slider,
                    onValueChange = { slider = it },
                    onValueChangeFinished = {
                        val from = slider.start.roundToInt().coerceIn(YearRange.MinYear, YearRange.MaxYear)
                        val to = slider.endInclusive.roundToInt().coerceIn(from, YearRange.MaxYear)
                        onYearRangeChange(YearRange(from, to))
                    },
                    valueRange = YearRange.MinYear.toFloat()..YearRange.MaxYear.toFloat(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .scale(scaleX = 1f, scaleY = 0.72f),
                    colors = SliderDefaults.colors(
                        thumbColor = ForestGreen,
                        activeTrackColor = ForestGreen,
                        inactiveTrackColor = CreamDeep,
                    ),
                )
            }
        }
    }
}

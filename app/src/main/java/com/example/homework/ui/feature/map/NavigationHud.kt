package com.example.homework.ui.feature.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.outlined.HeadsetMic
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homework.R
import com.example.homework.entity.map.NavigationState
import com.example.homework.entity.map.NavigationStatus
import com.example.homework.ui.locale.localizedNavigationDistance
import com.example.homework.ui.locale.navigationStatusLabel
import com.example.homework.ui.uikit.theme.ForestGreen
import com.example.homework.ui.uikit.theme.TextOnForest
import com.example.homework.ui.uikit.theme.TextPrimary
import com.example.homework.ui.uikit.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NavigationHud(
    navigation: NavigationState,
    currentPlaceName: String?,
    onPauseResume: () -> Unit,
    onRecenter: () -> Unit,
    onRetry: () -> Unit,
    onNext: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(bottom = 36.dp)
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(14.dp),
    ) {
        Text(
            text = currentPlaceName?.let { stringResource(R.string.nav_to_place, it) }
                ?: stringResource(R.string.nav_title),
            color = TextSecondary,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = navigation.instruction.ifBlank { navigationStatusLabel(navigation.status) },
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
        navigation.distanceToPlaceMeters?.let { meters ->
            Text(
                text = localizedNavigationDistance(meters.toDouble()),
                color = ForestGreen,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        navigation.message?.let { message ->
            Text(
                text = message,
                color = TextSecondary,
                fontSize = 13.sp,
            )
        }
        Spacer(Modifier.height(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (navigation.status) {
                NavigationStatus.Error, NavigationStatus.WaitingLocation ->
                    HudButton(
                        label = stringResource(R.string.action_retry),
                        onClick = onRetry,
                    )

                NavigationStatus.Arrived ->
                    HudButton(
                        label = stringResource(R.string.nav_next),
                        onClick = onNext,
                    )

                NavigationStatus.Finished ->
                    HudButton(
                        label = stringResource(R.string.nav_close),
                        onClick = onExit,
                    )

                NavigationStatus.Paused ->
                    HudButton(
                        label = stringResource(R.string.nav_resume),
                        onClick = onPauseResume,
                    )

                else ->
                    HudButton(
                        label = if (navigation.status == NavigationStatus.Navigating) {
                            stringResource(R.string.nav_pause)
                        } else {
                            stringResource(R.string.nav_waiting)
                        },
                        onClick = onPauseResume,
                        enabled = navigation.status == NavigationStatus.Navigating,
                    )
            }
            HudButton(
                label = stringResource(R.string.nav_me),
                onClick = onRecenter,
                filled = false,
            )
            HudButton(
                label = stringResource(R.string.nav_exit),
                onClick = onExit,
                filled = false,
            )
        }
    }
}

@Composable
fun AiGuideFab(
    enabled: Boolean,
    speaking: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.size(56.dp),
        shape = CircleShape,
        containerColor = if (enabled) ForestGreen else Color.White,
        contentColor = if (enabled) TextOnForest else ForestGreen,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
    ) {
        Icon(
            imageVector = when {
                speaking -> Icons.Filled.GraphicEq
                enabled -> Icons.Filled.HeadsetMic
                else -> Icons.Outlined.HeadsetMic
            },
            contentDescription = stringResource(
                if (enabled) R.string.nav_guide_on else R.string.nav_guide_off,
            ),
        )
    }
}

@Composable
private fun HudButton(
    label: String,
    filled: Boolean = true,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        color = if (!enabled) {
            TextSecondary
        } else if (filled) {
            TextOnForest
        } else {
            ForestGreen
        },
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (filled && enabled) ForestGreen else Color(0x14163833))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

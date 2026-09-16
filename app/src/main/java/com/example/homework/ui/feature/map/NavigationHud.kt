package com.example.homework.ui.feature.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homework.core.domain.navigation.formatNavigationDistance
import com.example.homework.entity.map.NavigationState
import com.example.homework.entity.map.NavigationStatus
import com.example.homework.ui.uikit.theme.ForestGreen
import com.example.homework.ui.uikit.theme.TextOnForest
import com.example.homework.ui.uikit.theme.TextPrimary
import com.example.homework.ui.uikit.theme.TextSecondary

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
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(14.dp),
    ) {
        Text(
            text = currentPlaceName?.let { "К точке: $it" } ?: "Навигация",
            color = TextSecondary,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = navigation.instruction.ifBlank { statusLabel(navigation.status) },
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
        navigation.distanceToPlaceMeters?.let { meters ->
            Text(
                text = formatNavigationDistance(meters.toDouble()),
                color = ForestGreen,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        navigation.message?.let { message ->
            Text(text = message, color = TextSecondary, fontSize = 13.sp)
        }
        Spacer(Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (navigation.status) {
                NavigationStatus.Error, NavigationStatus.WaitingLocation ->
                    HudButton("Повторить", onRetry)
                NavigationStatus.Arrived ->
                    HudButton("Далее", onNext)
                NavigationStatus.Finished ->
                    HudButton("Закрыть", onExit)
                NavigationStatus.Paused ->
                    HudButton("Продолжить", onPauseResume)
                else ->
                    HudButton(
                        if (navigation.status == NavigationStatus.Navigating) "Пауза" else "Ждём…",
                        onPauseResume,
                        enabled = navigation.status == NavigationStatus.Navigating,
                    )
            }
            HudButton("Я", onRecenter, filled = false)
            HudButton("Выйти", onExit, filled = false)
        }
    }
}

private fun statusLabel(status: NavigationStatus): String = when (status) {
    NavigationStatus.WaitingLocation -> "Ждём геолокацию"
    NavigationStatus.BuildingRoute -> "Строим маршрут"
    NavigationStatus.Navigating -> "Навигация"
    NavigationStatus.Paused -> "Пауза"
    NavigationStatus.Arrived -> "Вы на месте"
    NavigationStatus.Finished -> "Маршрут завершён"
    NavigationStatus.Error -> "Ошибка маршрута"
    NavigationStatus.Idle -> "Навигация"
}

@Composable
private fun HudButton(
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
            .background(if (filled && enabled) ForestGreen else Color(0x14163833))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

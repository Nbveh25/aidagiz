package com.example.homework.ui.locale

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.homework.R
import com.example.homework.entity.map.NavigationStatus

@Composable
fun localizedDistance(meters: Int): String =
    if (meters < 1000) {
        stringResource(R.string.distance_meters, meters)
    } else {
        stringResource(R.string.distance_kilometers, meters / 1000f)
    }

@Composable
fun localizedNavigationDistance(meters: Double): String {
    val value = meters.coerceAtLeast(0.0)
    return when {
        value >= 1000 -> stringResource(R.string.distance_kilometers, value / 1000.0)
        value >= 50 -> stringResource(R.string.distance_meters, (value / 10).toInt() * 10)
        else -> stringResource(R.string.distance_meters, (value / 5).toInt() * 5)
    }
}

@Composable
fun localizedPlacesCount(count: Int): String =
    pluralStringResource(R.plurals.status_places_nearby, count, count)

@Composable
fun navigationStatusLabel(status: NavigationStatus): String = stringResource(
    when (status) {
        NavigationStatus.WaitingLocation -> R.string.nav_status_waiting
        NavigationStatus.BuildingRoute -> R.string.nav_status_building
        NavigationStatus.Navigating -> R.string.nav_status_navigating
        NavigationStatus.Paused -> R.string.nav_status_paused
        NavigationStatus.Arrived -> R.string.nav_status_arrived
        NavigationStatus.Finished -> R.string.nav_status_finished
        NavigationStatus.Error -> R.string.nav_status_error
        NavigationStatus.Idle -> R.string.nav_title
    },
)

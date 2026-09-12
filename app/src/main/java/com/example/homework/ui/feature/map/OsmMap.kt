package com.example.homework.ui.feature.map

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.homework.OSM_USER_AGENT
import com.example.homework.R
import com.example.homework.entity.map.GeoLocation
import com.example.homework.entity.map.OsmPlace
import com.example.homework.entity.map.PlaceCategory
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable

@Composable
fun OsmMap(
    center: GeoLocation,
    user: GeoLocation?,
    places: List<OsmPlace>,
    selectedPlaceId: Long?,
    recenterToken: Int,
    onPlaceSelected: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val density = LocalDensity.current
    val iconSizePx = with(density) { 34.dp.roundToPx() }
    val holder = remember { OsmMapHolder() }
    holder.onPlaceSelected = onPlaceSelected

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            Configuration.getInstance().userAgentValue = OSM_USER_AGENT
            MapView(ctx).apply {
                setTileSource(AppTileSource.cartoVoyager)
                setMultiTouchControls(true)
                zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                minZoomLevel = 4.0
                maxZoomLevel = 19.0
                controller.setZoom(16.0)
                controller.setCenter(GeoPoint(center.lat, center.lon))
                overlays.add(
                    MapEventsOverlay(
                        object : MapEventsReceiver {
                            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                holder.onPlaceSelected(null)
                                return false
                            }

                            override fun longPressHelper(p: GeoPoint?): Boolean = false
                        },
                    ),
                )
                holder.mapView = this
            }
        },
        update = { mapView ->
            syncUserMarker(mapView, holder, user)
            syncPlaceMarkers(mapView, holder, places, selectedPlaceId, iconSizePx)
            if (holder.lastRecenterToken != recenterToken) {
                holder.lastRecenterToken = recenterToken
                val target = user ?: center
                mapView.controller.animateTo(GeoPoint(target.lat, target.lon))
            }
        },
        onRelease = { mapView ->
            mapView.onPause()
            mapView.onDetach()
            holder.mapView = null
        },
    )

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> holder.mapView?.onResume()
                Lifecycle.Event.ON_PAUSE -> holder.mapView?.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
}

private class OsmMapHolder {
    var mapView: MapView? = null
    var userMarker: Marker? = null
    val placeMarkers = mutableMapOf<Long, Marker>()
    var lastRecenterToken = -1
    var onPlaceSelected: (Long?) -> Unit = {}
}

private fun syncUserMarker(
    mapView: MapView,
    holder: OsmMapHolder,
    user: GeoLocation?,
) {
    if (user == null) {
        holder.userMarker?.let { mapView.overlays.remove(it) }
        holder.userMarker = null
        return
    }
    val marker = holder.userMarker ?: Marker(mapView).apply {
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        icon = ContextCompat.getDrawable(mapView.context, R.drawable.ic_user_puck)
        title = "Вы здесь"
        setOnMarkerClickListener { _, _ -> true }
        mapView.overlays.add(this)
        holder.userMarker = this
    }
    marker.position = GeoPoint(user.lat, user.lon)
    marker.rotation = -user.bearing
}

private fun syncPlaceMarkers(
    mapView: MapView,
    holder: OsmMapHolder,
    places: List<OsmPlace>,
    selectedPlaceId: Long?,
    iconSizePx: Int,
) {
    val visibleIds = places.map { it.id }.toSet()
    val iterator = holder.placeMarkers.iterator()
    while (iterator.hasNext()) {
        val (id, marker) = iterator.next()
        if (id !in visibleIds) {
            mapView.overlays.remove(marker)
            iterator.remove()
        }
    }
    val cache = mutableMapOf<PlaceCategory, Drawable>()
    places.forEach { place ->
        val marker = holder.placeMarkers.getOrPut(place.id) {
            Marker(mapView).apply {
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                setOnMarkerClickListener { _, _ ->
                    holder.onPlaceSelected(place.id)
                    true
                }
                mapView.overlays.add(this)
            }
        }
        marker.position = GeoPoint(place.lat, place.lon)
        marker.title = place.name
        marker.snippet = place.category.label
        marker.icon = cache.getOrPut(place.category) {
            emojiDrawable(mapView, place.category.emoji, iconSizePx)
        }
        marker.setInfoWindow(null)
        marker.setAlpha(if (place.id == selectedPlaceId || selectedPlaceId == null) 1f else 0.62f)
    }
    holder.userMarker?.let { userMarker ->
        mapView.overlays.remove(userMarker)
        mapView.overlays.add(userMarker)
    }
    mapView.invalidate()
}

private fun emojiDrawable(mapView: MapView, emoji: String, sizePx: Int): Drawable {
    val bitmap = createBitmap(sizePx, sizePx)
    val canvas = Canvas(bitmap)
    val background = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - 1f, background)
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF163833.toInt()
        style = Paint.Style.STROKE
        strokeWidth = sizePx * 0.06f
    }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - 1f, stroke)
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sizePx * 0.52f
        textAlign = Paint.Align.CENTER
    }
    val y = sizePx / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
    canvas.drawText(emoji, sizePx / 2f, y, textPaint)
    return bitmap.toDrawable(mapView.resources)
}

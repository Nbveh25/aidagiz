package com.example.homework.ui.feature.map

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.homework.OSM_USER_AGENT
import com.example.homework.entity.map.GeoLocation
import com.example.homework.entity.map.OsmPlace
import com.example.homework.entity.map.PlaceCategory
import com.example.homework.entity.map.RoutePlace
import com.example.homework.ui.locale.markerIconRes
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline
import kotlin.math.pow
import kotlin.math.roundToInt

@Composable
fun OsmMap(
    center: GeoLocation,
    user: GeoLocation?,
    places: List<OsmPlace>,
    routePlaces: List<RoutePlace>,
    selectedPlaceId: String?,
    routeGeometry: List<GeoLocation>?,
    routeDirty: Boolean,
    followUser: Boolean,
    recenterToken: Int,
    fitRouteToken: Int,
    onPlaceSelected: (String?) -> Unit,
    onUserMapInteraction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (LocalInspectionMode.current) {
        MapPreviewPlaceholder(
            center = center,
            user = user,
            places = places,
            routePlaces = routePlaces,
            routeGeometry = routeGeometry,
            modifier = modifier,
        )
        return
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val density = LocalDensity.current
    val iconSizePx = with(density) { 34.dp.roundToPx() }
    val selectedSizePx = with(density) { 44.dp.roundToPx() }
    val clusterSizePx = with(density) { 44.dp.roundToPx() }
    val routeSizePx = with(density) { 36.dp.roundToPx() }
    val strokeWidth = with(density) { 6.dp.toPx() }
    val holder = remember { OsmMapHolder() }
    holder.onPlaceSelected = onPlaceSelected
    holder.onUserMapInteraction = onUserMapInteraction

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
                controller.setZoom(13.0)
                controller.setCenter(GeoPoint(center.lat, center.lon))
                overlays.add(DragDetectOverlay(holder))
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
                addMapListener(
                    object : MapListener {
                        override fun onScroll(event: ScrollEvent?): Boolean = false

                        override fun onZoom(event: ZoomEvent?): Boolean {
                            holder.clusterZoom = zoomLevelDouble
                            holder.needsClusterRefresh = true
                            syncPlaceMarkers(
                                this@apply,
                                holder,
                                holder.places,
                                holder.selectedPlaceId,
                                holder.iconSizePx,
                                holder.selectedSizePx,
                                holder.clusterSizePx,
                            )
                            syncRouteMarkers(
                                this@apply,
                                holder,
                                holder.routePlaces,
                                holder.selectedPlaceId,
                                holder.routeSizePx,
                            )
                            invalidate()
                            return false
                        }
                    },
                )
                holder.mapView = this
                holder.clusterZoom = zoomLevelDouble
            }
        },
        update = { mapView ->
            holder.places = places
            holder.routePlaces = routePlaces
            holder.selectedPlaceId = selectedPlaceId
            holder.iconSizePx = iconSizePx
            holder.selectedSizePx = selectedSizePx
            holder.clusterSizePx = clusterSizePx
            holder.routeSizePx = routeSizePx
            syncRouteLine(mapView, holder, routeGeometry, routeDirty, strokeWidth)
            syncPlaceMarkers(
                mapView,
                holder,
                places,
                selectedPlaceId,
                iconSizePx,
                selectedSizePx,
                clusterSizePx,
            )
            syncRouteMarkers(mapView, holder, routePlaces, selectedPlaceId, routeSizePx)
            syncUserMarker(mapView, holder, user)
            if (followUser && user != null) {
                mapView.mapOrientation = user.bearing
                mapView.controller.setZoom(17.0)
                mapView.controller.animateTo(GeoPoint(user.lat, user.lon), 17.0, 600L)
            } else if (holder.lastRecenterToken != recenterToken) {
                holder.lastRecenterToken = recenterToken
                mapView.mapOrientation = 0f
                val target = user ?: center
                mapView.controller.animateTo(GeoPoint(target.lat, target.lon))
            }
            if (holder.lastFitRouteToken != fitRouteToken && !routeGeometry.isNullOrEmpty()) {
                holder.lastFitRouteToken = fitRouteToken
                val box = BoundingBox.fromGeoPoints(routeGeometry.map { GeoPoint(it.lat, it.lon) })
                mapView.post {
                    mapView.zoomToBoundingBox(box, true, 80, 17.0, 800)
                }
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
    var accuracyOverlay: AccuracyOverlay? = null
    var routeLine: Polyline? = null
    val placeMarkers = mutableMapOf<String, Marker>()
    val clusterMarkers = mutableListOf<Marker>()
    val routeMarkers = mutableMapOf<String, Marker>()
    val iconCache = mutableMapOf<MarkerIconKey, Drawable>()
    var lastRecenterToken = -1
    var lastFitRouteToken = -1
    var clusterZoom = 13.0
    var needsClusterRefresh = true
    var lastClusteredIds: List<String> = emptyList()
    var places: List<OsmPlace> = emptyList()
    var routePlaces: List<RoutePlace> = emptyList()
    var selectedPlaceId: String? = null
    var iconSizePx = 32
    var selectedSizePx = 42
    var clusterSizePx = 44
    var routeSizePx = 36
    var onPlaceSelected: (String?) -> Unit = {}
    var onUserMapInteraction: () -> Unit = {}
}

private data class MarkerIconKey(
    val color: Long,
    val iconRes: Int,
    val selected: Boolean,
    val size: Int,
)

private fun syncRouteLine(
    mapView: MapView,
    holder: OsmMapHolder,
    geometry: List<GeoLocation>?,
    routeDirty: Boolean,
    strokeWidth: Float,
) {
    if (geometry.isNullOrEmpty()) {
        holder.routeLine?.let { mapView.overlays.remove(it) }
        holder.routeLine = null
        return
    }
    val line = holder.routeLine ?: Polyline(mapView).apply {
        outlinePaint.color = 0xFF3F7D41.toInt()
        outlinePaint.strokeCap = Paint.Cap.ROUND
        outlinePaint.strokeJoin = Paint.Join.ROUND
        outlinePaint.isAntiAlias = true
        mapView.overlays.add(0, this)
        holder.routeLine = this
    }
    line.outlinePaint.strokeWidth = strokeWidth
    line.outlinePaint.alpha = if (routeDirty) (0.35f * 255).roundToInt() else (0.95f * 255).roundToInt()
    line.setPoints(geometry.map { GeoPoint(it.lat, it.lon) })
}

private fun syncPlaceMarkers(
    mapView: MapView,
    holder: OsmMapHolder,
    places: List<OsmPlace>,
    selectedPlaceId: String?,
    iconSizePx: Int,
    selectedSizePx: Int,
    clusterSizePx: Int,
) {
    val zoom = mapView.zoomLevelDouble
    val clustered = zoom < 14.0 && holder.routePlaces.isEmpty()
    val displayPlaces = places
    val ids = displayPlaces.map { it.id }
    if (!holder.needsClusterRefresh &&
        holder.clusterZoom == zoom &&
        holder.lastClusteredIds == ids &&
        holder.placeMarkers.keys == ids.toSet() &&
        !clustered
    ) {
        displayPlaces.forEach { place ->
            holder.placeMarkers[place.id]?.icon = markerDrawable(
                mapView,
                holder,
                place.category,
                selected = place.id == selectedPlaceId,
                sizePx = if (place.id == selectedPlaceId) selectedSizePx else iconSizePx,
            )
        }
        return
    }
    holder.needsClusterRefresh = false
    holder.clusterZoom = zoom
    holder.lastClusteredIds = ids

    holder.placeMarkers.values.forEach(mapView.overlays::remove)
    holder.placeMarkers.clear()
    holder.clusterMarkers.forEach(mapView.overlays::remove)
    holder.clusterMarkers.clear()

    if (!clustered) {
        displayPlaces.forEach { place ->
            val marker = Marker(mapView).apply {
                position = GeoPoint(place.lat, place.lon)
                title = place.name
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                setInfoWindow(null)
                icon = markerDrawable(
                    mapView,
                    holder,
                    place.category,
                    selected = place.id == selectedPlaceId,
                    sizePx = if (place.id == selectedPlaceId) selectedSizePx else iconSizePx,
                )
                setOnMarkerClickListener { _, _ ->
                    holder.onPlaceSelected(place.id)
                    true
                }
            }
            mapView.overlays.add(marker)
            holder.placeMarkers[place.id] = marker
        }
        return
    }

    val cell = 0.012 * (14.0 - zoom).coerceAtLeast(0.5)
    displayPlaces.groupBy { place ->
        (place.lat / cell).roundToInt() to (place.lon / cell).roundToInt()
    }.forEach { (_, group) ->
        if (group.size == 1) {
            val place = group.first()
            val marker = Marker(mapView).apply {
                position = GeoPoint(place.lat, place.lon)
                title = place.name
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                setInfoWindow(null)
                icon = markerDrawable(
                    mapView,
                    holder,
                    place.category,
                    selected = place.id == selectedPlaceId,
                    sizePx = iconSizePx,
                )
                setOnMarkerClickListener { _, _ ->
                    holder.onPlaceSelected(place.id)
                    true
                }
            }
            mapView.overlays.add(marker)
            holder.placeMarkers[place.id] = marker
        } else {
            val lat = group.map { it.lat }.average()
            val lon = group.map { it.lon }.average()
            val marker = Marker(mapView).apply {
                position = GeoPoint(lat, lon)
                title = "${group.size} мест"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                setInfoWindow(null)
                icon = clusterDrawable(mapView, group.size, clusterSizePx)
                setOnMarkerClickListener { _, _ ->
                    mapView.controller.animateTo(
                        GeoPoint(lat, lon),
                        (mapView.zoomLevelDouble + 1.8).coerceAtMost(16.0),
                        400L,
                    )
                    true
                }
            }
            mapView.overlays.add(marker)
            holder.clusterMarkers += marker
        }
    }
}

private fun syncRouteMarkers(
    mapView: MapView,
    holder: OsmMapHolder,
    routePlaces: List<RoutePlace>,
    selectedPlaceId: String?,
    sizePx: Int,
) {
    val visibleIds = routePlaces.map { it.id }.toSet()
    val iterator = holder.routeMarkers.iterator()
    while (iterator.hasNext()) {
        val (id, marker) = iterator.next()
        if (id !in visibleIds) {
            mapView.overlays.remove(marker)
            iterator.remove()
        }
    }
    routePlaces.forEach { routePlace ->
        val marker = holder.routeMarkers.getOrPut(routePlace.id) {
            Marker(mapView).apply {
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                setInfoWindow(null)
                setOnMarkerClickListener { _, _ ->
                    holder.onPlaceSelected(routePlace.id)
                    true
                }
                mapView.overlays.add(this)
            }
        }
        marker.position = GeoPoint(routePlace.lat, routePlace.lon)
        marker.title = routePlace.name
        val selected = routePlace.id == selectedPlaceId
        marker.icon = markerDrawable(
            mapView,
            holder,
            routePlace.category,
            selected = selected,
            sizePx = if (selected) holder.selectedSizePx else sizePx,
        )
    }
}

private fun syncUserMarker(
    mapView: MapView,
    holder: OsmMapHolder,
    user: GeoLocation?,
) {
    if (user == null) {
        holder.userMarker?.let { mapView.overlays.remove(it) }
        holder.accuracyOverlay?.let { mapView.overlays.remove(it) }
        holder.userMarker = null
        holder.accuracyOverlay = null
        return
    }
    val accuracy = holder.accuracyOverlay ?: AccuracyOverlay().also {
        mapView.overlays.add(it)
        holder.accuracyOverlay = it
    }
    accuracy.location = user
    val marker = holder.userMarker ?: Marker(mapView).apply {
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        setInfoWindow(null)
        setOnMarkerClickListener { _, _ -> true }
        mapView.overlays.add(this)
        holder.userMarker = this
    }
    marker.position = GeoPoint(user.lat, user.lon)
    marker.rotation = -user.bearing
    marker.icon = userDrawable(mapView, 22)
    holder.userMarker?.let { userMarker ->
        mapView.overlays.remove(userMarker)
        mapView.overlays.add(userMarker)
    }
    mapView.invalidate()
}

private fun markerDrawable(
    mapView: MapView,
    holder: OsmMapHolder,
    category: PlaceCategory,
    selected: Boolean,
    sizePx: Int,
): Drawable {
    val key = MarkerIconKey(category.markerColor, category.markerIconRes, selected, sizePx)
    return holder.iconCache.getOrPut(key) {
        circleIconDrawable(
            mapView = mapView,
            color = category.markerColor.toInt(),
            iconRes = category.markerIconRes,
            sizePx = sizePx,
            strokeWidth = if (selected) sizePx * 0.12f else sizePx * 0.07f,
        )
    }
}

private fun circleIconDrawable(
    mapView: MapView,
    color: Int,
    iconRes: Int,
    sizePx: Int,
    strokeWidth: Float,
): Drawable {
    val bitmap = createBitmap(sizePx, sizePx)
    val canvas = Canvas(bitmap)
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = 0xFFFFFFFF.toInt()
        style = Paint.Style.STROKE
        this.strokeWidth = strokeWidth
    }
    val radius = sizePx / 2f - strokeWidth
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, radius, fill)
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, radius, stroke)
    val icon = ContextCompat.getDrawable(mapView.context, iconRes)?.mutate()
    if (icon != null) {
        val padding = (sizePx * 0.22f).roundToInt()
        icon.setBounds(padding, padding, sizePx - padding, sizePx - padding)
        icon.setTint(0xFFFFFFFF.toInt())
        icon.draw(canvas)
    }
    return bitmap.toDrawable(mapView.resources)
}

private fun clusterDrawable(mapView: MapView, count: Int, sizePx: Int): Drawable =
    circleLabelDrawable(
        mapView = mapView,
        color = 0xFF3F7D41.toInt(),
        label = count.toString(),
        sizePx = sizePx,
        strokeWidth = sizePx * 0.06f,
    )

private fun userDrawable(mapView: MapView, sizePx: Int): Drawable {
    val bitmap = createBitmap(sizePx, sizePx)
    val canvas = Canvas(bitmap)
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2F80FF.toInt() }
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = sizePx * 0.18f
    }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - stroke.strokeWidth, fill)
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - stroke.strokeWidth, stroke)
    return bitmap.toDrawable(mapView.resources)
}

private fun circleLabelDrawable(
    mapView: MapView,
    color: Int,
    label: String,
    sizePx: Int,
    strokeWidth: Float,
): Drawable {
    val bitmap = createBitmap(sizePx, sizePx)
    val canvas = Canvas(bitmap)
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = 0xFFFFFFFF.toInt()
        style = Paint.Style.STROKE
        this.strokeWidth = strokeWidth
    }
    val radius = sizePx / 2f - strokeWidth
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, radius, fill)
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, radius, stroke)
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = 0xFFFFFFFF.toInt()
        textAlign = Paint.Align.CENTER
        textSize = sizePx * if (label.length > 1) 0.38f else 0.46f
        isFakeBoldText = true
    }
    val y = sizePx / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
    canvas.drawText(label, sizePx / 2f, y, textPaint)
    return bitmap.toDrawable(mapView.resources)
}

private class AccuracyOverlay : Overlay() {
    var location: GeoLocation? = null

    override fun draw(canvas: Canvas, projection: Projection) {
        val user = location ?: return
        val point = projection.toPixels(GeoPoint(user.lat, user.lon), Point())
        val radius = user.accuracyMeters.coerceIn(16f, 80f)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x333F7D41
        }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x663F7D41
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), radius, fill)
        canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), radius, stroke)
    }
}

private class DragDetectOverlay(
    private val holder: OsmMapHolder,
) : Overlay() {
    private var downX = 0f
    private var downY = 0f
    private var dragged = false

    override fun onTouchEvent(event: MotionEvent, mapView: MapView): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                dragged = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - downX
                val dy = event.y - downY
                if (!dragged && dx * dx + dy * dy > 25f.pow(2)) {
                    dragged = true
                    holder.onUserMapInteraction()
                }
            }
        }
        return false
    }
}

@Composable
private fun MapPreviewPlaceholder(
    center: GeoLocation,
    user: GeoLocation?,
    places: List<OsmPlace>,
    routePlaces: List<RoutePlace>,
    routeGeometry: List<GeoLocation>?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFE7E4DC)),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val scale = 12_000f
            fun point(location: GeoLocation): Offset = Offset(
                x = size.width / 2f + ((location.lon - center.lon) * scale).toFloat(),
                y = size.height / 2f - ((location.lat - center.lat) * scale).toFloat(),
            )
            for (step in 1..8) {
                val y = size.height * step / 9f
                val x = size.width * step / 9f
                drawLine(Color(0x33163833), Offset(0f, y), Offset(size.width, y), 2f)
                drawLine(Color(0x33163833), Offset(x, 0f), Offset(x, size.height), 2f)
            }
            val geometry = routeGeometry.orEmpty()
            if (geometry.size >= 2) {
                for (index in 1 until geometry.size) {
                    drawLine(
                        color = Color(0xFF3F7D41),
                        start = point(geometry[index - 1]),
                        end = point(geometry[index]),
                        strokeWidth = 10f,
                        cap = StrokeCap.Round,
                    )
                }
            }
            places.forEach { place ->
                drawCircle(
                    color = Color(place.category.markerColor),
                    radius = 16f,
                    center = point(GeoLocation(place.lat, place.lon)),
                )
            }
            routePlaces.forEach { stop ->
                drawCircle(
                    color = Color(0xFF3F7D41),
                    radius = 20f,
                    center = point(stop.location),
                    style = Stroke(width = 5f, join = StrokeJoin.Round),
                )
            }
            user?.let { location ->
                drawCircle(Color(0x332F80FF), 48f, point(location))
                drawCircle(Color(0xFF2F80FF), 14f, point(location))
            }
        }
    }
}


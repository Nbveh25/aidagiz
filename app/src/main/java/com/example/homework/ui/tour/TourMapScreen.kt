package com.example.homework.ui.tour

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.homework.R
import com.example.homework.ui.uikit.component.RecenterChip
import com.example.homework.ui.uikit.theme.HomeworkTheme
import com.example.homework.ui.uikit.theme.MapScrim
import com.example.homework.ui.uikit.theme.RouteGlow
import com.example.homework.ui.uikit.theme.RouteGreen
import com.example.homework.ui.uikit.theme.UserBlue

@Composable
fun TourMapScreen(
    state: TourMapUiState = PreviewTourMapState,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MapScrim),
    ) {
        Image(
            painter = painterResource(R.drawable.map_kazan),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = BiasAlignment(-0.42f, -0.12f),
            modifier = Modifier.fillMaxSize(),
        )
        TourRouteOverlay(
            points = state.route,
            user = state.user,
            modifier = Modifier.fillMaxSize(),
        )
        state.stops.forEach { stop ->
            MapPlaceCard(
                title = stop.title,
                minutes = stop.minutes,
                photoRes = stop.photoRes,
                modifier = Modifier.offset(
                    x = maxWidth * stop.x,
                    y = maxHeight * stop.y,
                ),
            )
        }
        UserPuck(
            modifier = Modifier.offset(
                x = maxWidth * state.user.x - 46.dp,
                y = maxHeight * state.user.y - 46.dp,
            ),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp),
        ) {
            NavigationBanner(
                distanceMeters = state.distanceMeters,
                instructionPrimary = state.instructionPrimary,
                instructionSecondary = state.instructionSecondary,
                photoRes = state.instructionPhotoRes,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        MapControlColumn(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 108.dp, end = 16.dp),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        ) {
            RecenterChip(
                modifier = Modifier.padding(start = 16.dp, bottom = 12.dp),
            )
            TourBottomPanel(
                title = state.currentTitle,
                description = state.currentDescription,
                photoRes = state.currentPhotoRes,
                stepIndex = state.stepIndex,
                stepCount = state.stepCount,
                remainingLabel = state.remainingLabel,
            )
        }
    }
}

@Composable
private fun TourRouteOverlay(
    points: List<Offset>,
    user: Offset,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val stroke = with(density) { 6.dp.toPx() }
    val dash = with(density) { 14.dp.toPx() }
    val gap = with(density) { 10.dp.toPx() }
    val glowStroke = with(density) { 10.dp.toPx() }
    val filledRadius = with(density) { 7.dp.toPx() }
    val ringRadius = with(density) { 7.dp.toPx() }
    val ringWidth = with(density) { 3.dp.toPx() }

    Canvas(modifier) {
        if (points.size < 2) return@Canvas
        val mapped = points.map { Offset(it.x * size.width, it.y * size.height) }
        val path = Path().apply {
            moveTo(mapped.first().x, mapped.first().y)
            for (i in 1 until mapped.size) {
                val prev = mapped[i - 1]
                val curr = mapped[i]
                val mid = Offset((prev.x + curr.x) / 2f, (prev.y + curr.y) / 2f)
                quadraticTo(prev.x, prev.y, mid.x, mid.y)
            }
            lineTo(mapped.last().x, mapped.last().y)
        }
        drawPath(
            path = path,
            color = RouteGreen,
            style = Stroke(
                width = stroke,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, gap)),
            ),
        )
        val glowStart = mapped[4]
        val glowEnd = mapped[6]
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(RouteGlow.copy(alpha = 0.15f), RouteGlow, RouteGlow.copy(alpha = 0.2f)),
                start = glowStart,
                end = glowEnd,
            ),
            start = glowStart,
            end = glowEnd,
            strokeWidth = glowStroke,
            cap = StrokeCap.Round,
        )
        val stopDots = listOf(
            mapped.first() to true,
            mapped[2] to false,
            mapped[7] to false,
            mapped.last() to true,
        )
        stopDots.forEach { (center, filled) ->
            if (filled) {
                drawCircle(color = Color.White, radius = filledRadius + ringWidth, center = center)
                drawCircle(color = RouteGreen, radius = filledRadius, center = center)
            } else {
                drawCircle(color = Color.White, radius = ringRadius, center = center)
                drawCircle(
                    color = RouteGreen,
                    radius = ringRadius,
                    center = center,
                    style = Stroke(width = ringWidth),
                )
            }
        }
        val userCenter = Offset(user.x * size.width, user.y * size.height)
        drawCircle(
            color = UserBlue.copy(alpha = 0.18f),
            radius = with(density) { 36.dp.toPx() },
            center = userCenter,
        )
    }
}

@Composable
private fun UserPuck(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(92.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            UserBlue.copy(alpha = 0.38f),
                            UserBlue.copy(alpha = 0.12f),
                            Color.Transparent,
                        ),
                    ),
                    shape = CircleShape,
                ),
        )
        Icon(
            imageVector = Icons.Filled.Navigation,
            contentDescription = null,
            tint = RouteGreen,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
                .size(16.dp)
                .rotate(-8f),
        )
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(UserBlue, CircleShape),
            )
        }
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    device = "spec:width=411dp,height=891dp,dpi=420",
)
@Composable
private fun TourMapScreenPreview() {
    HomeworkTheme {
        TourMapScreen()
    }
}

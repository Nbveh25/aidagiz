package com.example.homework.ui.feature.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homework.R
import com.example.homework.entity.guide.SpeechStatus
import com.example.homework.ui.uikit.theme.ForestGreenDeep
import com.example.homework.ui.uikit.theme.SheetWhite
import com.example.homework.ui.uikit.theme.TextOnForest
import com.example.homework.ui.uikit.theme.TextPrimary
import com.example.homework.ui.uikit.theme.TextSecondary

@Composable
fun RouteSummaryOverlay(
    text: String,
    visible: Boolean,
    expanded: Boolean,
    speech: SpeechStatus,
    finished: Boolean,
    showReopen: Boolean,
    onDismiss: () -> Unit,
    onReopen: () -> Unit,
    onToggleExpanded: () -> Unit,
    onToggleSpeech: () -> Unit,
    onRetrySpeech: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (visible) {
        RouteSummaryCard(
            text = text,
            expanded = expanded,
            speech = speech,
            finished = finished,
            onDismiss = onDismiss,
            onToggleExpanded = onToggleExpanded,
            onToggleSpeech = onToggleSpeech,
            onRetrySpeech = onRetrySpeech,
            modifier = modifier,
        )
    } else if (showReopen) {
        Row(
            modifier = modifier
                .shadow(6.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .clickable(onClick = onReopen)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.MenuBook,
                contentDescription = null,
                tint = ForestGreenDeep,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.route_summary_reopen),
                color = ForestGreenDeep,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun RouteSummaryCard(
    text: String,
    expanded: Boolean,
    speech: SpeechStatus,
    finished: Boolean,
    onDismiss: () -> Unit,
    onToggleExpanded: () -> Unit,
    onToggleSpeech: () -> Unit,
    onRetrySpeech: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(SheetWhite)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.route_summary_title),
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.guide_close),
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.size(6.dp))
        Text(
            text = text,
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            maxLines = if (expanded) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis,
            modifier = if (expanded) {
                Modifier
                    .heightIn(max = 160.dp)
                    .verticalScroll(rememberScrollState())
            } else {
                Modifier
            },
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = stringResource(
                if (expanded) R.string.guide_hide_text else R.string.route_summary_read_all,
            ),
            color = ForestGreenDeep,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable(onClick = onToggleExpanded),
        )
        Spacer(Modifier.size(10.dp))
        when (speech) {
            SpeechStatus.Error -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.route_summary_audio_error),
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringResource(R.string.route_summary_retry),
                        color = ForestGreenDeep,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable(onClick = onRetrySpeech),
                    )
                }
            }
            else -> {
                val label = when (speech) {
                    SpeechStatus.Loading -> stringResource(R.string.route_summary_preparing)
                    SpeechStatus.Playing -> stringResource(R.string.guide_pause)
                    SpeechStatus.Paused -> stringResource(R.string.nav_resume)
                    SpeechStatus.Ready -> stringResource(
                        if (finished) R.string.route_summary_replay else R.string.route_summary_listen,
                    )
                    else -> stringResource(R.string.route_summary_listen)
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(ForestGreenDeep)
                        .clickable(
                            enabled = speech != SpeechStatus.Loading && speech != SpeechStatus.Idle,
                            onClick = onToggleSpeech,
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (speech == SpeechStatus.Loading) {
                        CircularProgressIndicator(
                            color = TextOnForest,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = label,
                        color = TextOnForest,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

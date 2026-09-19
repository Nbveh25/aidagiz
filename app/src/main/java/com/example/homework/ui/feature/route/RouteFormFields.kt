package com.example.homework.ui.feature.route

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homework.R
import com.example.homework.entity.tour.CulturalInterest
import com.example.homework.entity.tour.RouteFormState
import com.example.homework.entity.tour.WalkPace
import com.example.homework.ui.locale.labelRes
import com.example.homework.ui.uikit.theme.CreamDeep
import com.example.homework.ui.uikit.theme.ForestGreen
import com.example.homework.ui.uikit.theme.GoldAccent
import com.example.homework.ui.uikit.theme.TextOnForest
import com.example.homework.ui.uikit.theme.TextPrimary
import com.example.homework.ui.uikit.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RouteFormFields(
    form: RouteFormState,
    onDuration: (Int) -> Unit,
    onToggleInterest: (String) -> Unit,
    onPace: (WalkPace) -> Unit,
    onAiRequest: (String) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        FormLabel(stringResource(R.string.builder_duration))
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RouteFormState.DURATION_OPTIONS.forEach { minutes ->
                ChoiceChip(
                    label = stringResource(R.string.builder_duration_hours, minutes / 60),
                    selected = form.durationMinutes == minutes,
                    onClick = { onDuration(minutes) },
                )
            }
        }
        Spacer(Modifier.height(if (compact) 14.dp else 18.dp))
        FormLabel(stringResource(R.string.builder_interests))
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CulturalInterest.entries.forEach { interest ->
                ChoiceChip(
                    label = stringResource(interest.labelRes),
                    selected = interest.apiValue in form.interests,
                    onClick = { onToggleInterest(interest.apiValue) },
                )
            }
        }
        Spacer(Modifier.height(if (compact) 14.dp else 18.dp))
        FormLabel(stringResource(R.string.builder_pace))
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            WalkPace.entries.forEach { pace ->
                ChoiceChip(
                    label = stringResource(pace.labelRes),
                    selected = form.pace == pace,
                    onClick = { onPace(pace) },
                )
            }
        }
        Spacer(Modifier.height(if (compact) 14.dp else 18.dp))
        FormLabel(
            text = stringResource(R.string.builder_wish),
            hint = stringResource(R.string.builder_wish_optional),
        )
        Spacer(Modifier.height(8.dp))
        WishField(
            value = form.aiRequest,
            onValueChange = onAiRequest,
            placeholder = stringResource(R.string.builder_wish_placeholder),
        )
    }
}

@Composable
fun WishField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CreamDeep)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        if (value.isBlank()) {
            Text(placeholder, color = TextSecondary, fontSize = 14.sp)
        }
        BasicTextField(
            value = value,
            onValueChange = { onValueChange(it.take(1_000)) },
            textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp, lineHeight = 20.sp),
            cursorBrush = SolidColor(ForestGreen),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        color = if (selected) TextOnForest else ForestGreen,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) ForestGreen else CreamDeep)
            .then(
                if (selected) {
                    Modifier
                } else {
                    Modifier.border(1.dp, GoldAccent.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
private fun FormLabel(
    text: String,
    hint: String? = null,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = text,
            color = ForestGreen,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
        if (hint != null) {
            Text(
                text = hint,
                color = TextSecondary,
                fontSize = 13.sp,
            )
        }
    }
}

package com.example.homework.ui.uikit.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homework.R
import com.example.homework.core.locale.AppLanguage
import com.example.homework.ui.uikit.theme.ForestGreen
import com.example.homework.ui.uikit.theme.TextOnForest
import com.example.homework.ui.uikit.theme.TextPrimary

@Composable
fun LanguageToggle(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.language_content_description)
    Row(
        modifier = modifier
            .semantics { contentDescription = description }
            .shadow(8.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .padding(4.dp),
    ) {
        LanguageChip(
            label = stringResource(R.string.language_russian),
            selected = selected == AppLanguage.Russian,
            onClick = { onSelect(AppLanguage.Russian) },
        )
        LanguageChip(
            label = stringResource(R.string.language_tatar),
            selected = selected == AppLanguage.Tatar,
            onClick = { onSelect(AppLanguage.Tatar) },
        )
    }
}

@Composable
private fun LanguageChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        color = if (selected) TextOnForest else TextPrimary,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) ForestGreen else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    )
}

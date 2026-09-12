package com.example.homework.ui.uikit.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homework.ui.uikit.theme.ForestGreen
import com.example.homework.ui.uikit.theme.TextOnForest

@Composable
fun StatusChip(
    title: String,
    subtitle: String,
    loading: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .shadow(10.dp, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .background(ForestGreen)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextOnForest,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitle,
                color = TextOnForest.copy(alpha = 0.92f),
                fontSize = 13.sp,
            )
        }
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = TextOnForest,
                strokeWidth = 2.dp,
            )
        }
    }
}
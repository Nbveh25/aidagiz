package com.example.homework.ui.feature.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homework.R
import com.example.homework.core.locale.AppLanguage
import com.example.homework.ui.uikit.component.LanguageToggle
import com.example.homework.ui.uikit.theme.Cream
import com.example.homework.ui.uikit.theme.ForestGreen
import com.example.homework.ui.uikit.theme.SerifFamily
import com.example.homework.ui.uikit.theme.TextSecondary

@Composable
fun PlaceholderPage(
    title: String,
    language: AppLanguage,
    onLanguageSelect: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Cream)
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        LanguageToggle(
            selected = language,
            onSelect = onLanguageSelect,
            modifier = Modifier.align(Alignment.End),
        )
        Column(Modifier.padding(horizontal = 8.dp, vertical = 16.dp)) {
            Text(
                text = title,
                color = ForestGreen,
                fontFamily = SerifFamily,
                fontSize = 34.sp,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.coming_soon_body),
                color = TextSecondary,
                fontSize = 16.sp,
                lineHeight = 22.sp,
            )
        }
    }
}

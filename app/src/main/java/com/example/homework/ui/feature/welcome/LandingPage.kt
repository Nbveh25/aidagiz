package com.example.homework.ui.feature.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homework.R
import com.example.homework.core.locale.AppLanguage
import com.example.homework.ui.app.AppDestination
import com.example.homework.ui.uikit.component.LanguageToggle
import com.example.homework.ui.uikit.theme.Cream
import com.example.homework.ui.uikit.theme.ForestGreen
import com.example.homework.ui.uikit.theme.GoldAccent
import com.example.homework.ui.uikit.theme.HomeworkTheme
import com.example.homework.ui.uikit.theme.SerifFamily
import com.example.homework.ui.uikit.theme.TextOnForest

@Composable
fun LandingPage(
    language: AppLanguage,
    onLanguageSelect: (AppLanguage) -> Unit,
    onNavigate: (AppDestination) -> Unit,
    onStartRoute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.photo_kremlin),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color(0x66163833),
                        0.45f to Color(0x33163833),
                        1f to Cream.copy(alpha = 0.96f),
                    ),
                ),
        )
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.weight(1f))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
            ) {
                Text(
                    text = stringResource(R.string.landing_kicker).uppercase(),
                    color = GoldAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.4.sp,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    color = ForestGreen,
                    fontFamily = SerifFamily,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 48.sp,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.landing_subtitle),
                    color = ForestGreen.copy(alpha = 0.78f),
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                )
                Spacer(Modifier.height(24.dp))
                LandingButton(
                    label = stringResource(R.string.landing_cta),
                    onClick = onStartRoute,
                    filled = true,
                )
                Spacer(Modifier.height(10.dp))
                LandingButton(
                    label = stringResource(R.string.landing_cta_map),
                    onClick = { onNavigate(AppDestination.Map) },
                    filled = false,
                )
            }
        }
        LanguageToggle(
            selected = language,
            onSelect = onLanguageSelect,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp),
        )
    }
}

@Composable
private fun LandingButton(
    label: String,
    onClick: () -> Unit,
    filled: Boolean,
) {
    Text(
        text = label,
        color = if (filled) TextOnForest else ForestGreen,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (filled) ForestGreen else ForestGreen.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        textAlign = TextAlign.Center,
    )
}

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun LandingPagePreview() {
    HomeworkTheme {
        LandingPage(
            language = AppLanguage.Russian,
            onLanguageSelect = {},
            onNavigate = {},
            onStartRoute = {},
        )
    }
}

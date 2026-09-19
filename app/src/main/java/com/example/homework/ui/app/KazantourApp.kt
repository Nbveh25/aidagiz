package com.example.homework.ui.app

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.homework.R
import com.example.homework.core.locale.LocaleStore
import com.example.homework.ui.feature.map.LiveMapScreen
import com.example.homework.ui.feature.map.LiveMapViewModel
import com.example.homework.ui.feature.route.RouteBuilderV2Page
import com.example.homework.ui.feature.welcome.LandingPage
import com.example.homework.ui.feature.welcome.PlaceholderPage
import org.koin.androidx.compose.koinViewModel

@Composable
fun KazantourApp(
    modifier: Modifier = Modifier,
    viewModel: LiveMapViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val localeStore = remember { LocaleStore(context) }
    val language = remember { localeStore.get() }
    var destination by rememberSaveable { mutableStateOf(AppDestination.Home) }

    val onLanguageSelect: (com.example.homework.core.locale.AppLanguage) -> Unit = { selected ->
        if (selected != language) {
            localeStore.set(selected)
            (context as? Activity)?.recreate()
        }
    }
    val onNavigate: (AppDestination) -> Unit = { selected ->
        destination = selected
    }

    BackHandler(enabled = destination != AppDestination.Home) {
        destination = AppDestination.Home
    }

    when (destination) {
        AppDestination.Home -> LandingPage(
            language = language,
            onLanguageSelect = onLanguageSelect,
            onNavigate = onNavigate,
            onStartRoute = {
                viewModel.openRouteBuilder()
                destination = AppDestination.RouteBuilder
            },
            modifier = modifier,
        )

        AppDestination.Map -> LiveMapScreen(
            viewModel = viewModel,
            language = language,
            onLanguageSelect = onLanguageSelect,
            modifier = modifier,
        )

        AppDestination.Events -> PlaceholderPage(
            title = stringResource(R.string.nav_events),
            language = language,
            onLanguageSelect = onLanguageSelect,
            modifier = modifier,
        )

        AppDestination.Tours -> PlaceholderPage(
            title = stringResource(R.string.nav_tours),
            language = language,
            onLanguageSelect = onLanguageSelect,
            modifier = modifier,
        )

        AppDestination.Profile -> PlaceholderPage(
            title = stringResource(R.string.nav_profile),
            language = language,
            onLanguageSelect = onLanguageSelect,
            modifier = modifier,
        )

        AppDestination.RouteBuilder -> RouteBuilderV2Page(
            language = language,
            onLanguageSelect = onLanguageSelect,
            viewModel = viewModel,
            modifier = modifier,
        )
    }
}

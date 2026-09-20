package com.example.homework.di

import com.example.homework.core.data.repository.AiGuideRepositoryImpl
import com.example.homework.core.data.repository.AnonymousUserRepositoryImpl
import com.example.homework.core.data.repository.LocationRatingRepositoryImpl
import com.example.homework.core.data.repository.OsrmRepositoryImpl
import com.example.homework.core.data.repository.PlaceDetailsRepositoryImpl
import com.example.homework.core.data.repository.HistoricalPlacesRepositoryImpl
import com.example.homework.core.data.repository.PlacesRepositoryImpl
import com.example.homework.core.data.repository.RouteRepositoryImpl
import com.example.homework.core.data.repository.TourRepositoryImpl
import com.example.homework.core.data.repository.UserLocationRepositoryImpl
import com.example.homework.core.data.repository.VoiceReadingRepositoryImpl
import com.example.homework.core.data.source.api.GuideApiClient
import com.example.homework.core.data.source.api.GuideApiConfig
import com.example.homework.core.data.source.api.GuideApiReachability
import com.example.homework.core.data.source.guide.AiGuideSource
import com.example.homework.core.data.source.guide.RouteSummarySource
import com.example.homework.core.data.source.guide.VoiceReadingSource
import com.example.homework.core.data.source.osrm.OsrmSource
import com.example.homework.core.data.source.overpass.OverpassSource
import com.example.homework.core.data.source.place.PlaceDetailsSource
import com.example.homework.core.data.source.place.PlaceImageSource
import com.example.homework.core.data.source.place.PlaceStorySource
import com.example.homework.core.data.source.place.HistoricalPlacesApiSource
import com.example.homework.core.data.source.place.PlacesApiSource
import com.example.homework.core.data.source.rating.LocationRatingSource
import com.example.homework.core.data.source.route.RouteApiSource
import com.example.homework.core.data.source.tour.TourSource
import com.example.homework.core.data.source.user.AnonymousUserSource
import com.example.homework.core.data.source.user.UserIdStore
import com.example.homework.core.data.source.userLocation.UserLocationSource
import com.example.homework.core.locale.AppStrings
import com.example.homework.core.locale.LocaleStore
import com.example.homework.core.domain.repository.AiGuideRepository
import com.example.homework.core.domain.repository.AnonymousUserRepository
import com.example.homework.core.domain.repository.LocationRatingRepository
import com.example.homework.core.domain.repository.OsrmRepository
import com.example.homework.core.domain.repository.PlaceDetailsRepository
import com.example.homework.core.domain.repository.HistoricalPlacesRepository
import com.example.homework.core.domain.repository.PlacesRepository
import com.example.homework.core.domain.repository.RouteRepository
import com.example.homework.core.domain.repository.TourRepository
import com.example.homework.core.domain.repository.UserLocationRepository
import com.example.homework.core.domain.repository.VoiceReadingRepository
import com.example.homework.core.domain.usecase.BuildAdventureRouteUseCase
import com.example.homework.core.domain.usecase.ContinueRouteUseCase
import com.example.homework.core.domain.usecase.ControlAiGuideUseCase
import com.example.homework.core.domain.usecase.EnsureAnonymousUserUseCase
import com.example.homework.core.domain.usecase.GetAiGuideUseCase
import com.example.homework.core.domain.usecase.GetHistoricalPlaceDetailsUseCase
import com.example.homework.core.domain.usecase.GetHistoricalPlacesUseCase
import com.example.homework.core.domain.usecase.GetNearbyPlacesUseCase
import com.example.homework.core.domain.usecase.GetOsrmRouteUseCase
import com.example.homework.core.domain.usecase.GetPlaceDetailsUseCase
import com.example.homework.core.domain.usecase.GetTourProgressUseCase
import com.example.homework.core.domain.usecase.GetUserLocationUseCase
import com.example.homework.core.domain.usecase.GoToNextStopUseCase
import com.example.homework.core.domain.usecase.GoToPreviousStopUseCase
import com.example.homework.core.domain.usecase.MarkPlaceVisitedUseCase
import com.example.homework.core.domain.usecase.ObserveAiGuidePlaybackUseCase
import com.example.homework.core.domain.usecase.OptimizeRoutePlacesUseCase
import com.example.homework.core.domain.usecase.RatePlaceUseCase
import com.example.homework.core.domain.usecase.RebuildAdventureRouteUseCase
import com.example.homework.core.domain.usecase.impl.BuildAdventureRouteUseCaseImpl
import com.example.homework.core.domain.usecase.impl.ContinueRouteUseCaseImpl
import com.example.homework.core.domain.usecase.impl.RebuildAdventureRouteUseCaseImpl
import com.example.homework.core.domain.usecase.impl.ControlAiGuideUseCaseImpl
import com.example.homework.core.domain.usecase.impl.EnsureAnonymousUserUseCaseImpl
import com.example.homework.core.domain.usecase.impl.GetAiGuideUseCaseImpl
import com.example.homework.core.domain.usecase.impl.GetHistoricalPlaceDetailsUseCaseImpl
import com.example.homework.core.domain.usecase.impl.GetHistoricalPlacesUseCaseImpl
import com.example.homework.core.domain.usecase.impl.GetNearbyPlacesUseCaseImpl
import com.example.homework.core.domain.usecase.impl.GetOsrmRouteUseCaseImpl
import com.example.homework.core.domain.usecase.impl.GetPlaceDetailsUseCaseImpl
import com.example.homework.core.domain.usecase.impl.GetTourProgressUseCaseImpl
import com.example.homework.core.domain.usecase.impl.GetUserLocationUseCaseImpl
import com.example.homework.core.domain.usecase.impl.GoToNextStopUseCaseImpl
import com.example.homework.core.domain.usecase.impl.GoToPreviousStopUseCaseImpl
import com.example.homework.core.domain.usecase.impl.MarkPlaceVisitedUseCaseImpl
import com.example.homework.core.domain.usecase.impl.ObserveAiGuidePlaybackUseCaseImpl
import com.example.homework.core.domain.usecase.impl.OptimizeRoutePlacesUseCaseImpl
import com.example.homework.core.domain.usecase.impl.RatePlaceUseCaseImpl
import com.example.homework.ui.feature.map.LiveMapViewModel
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

val dataModule = module {
    single {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .callTimeout(300, TimeUnit.SECONDS)
            .build()
    }

    single {
        val sharedClient = get<OkHttpClient>()
        GuideApiClient(
            httpClient = sharedClient.newBuilder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)
                .callTimeout(180, TimeUnit.SECONDS)
                .build(),
            baseUrl = GuideApiConfig.BASE_URL,
        )
    }
    single { UserIdStore(androidContext()) }
    single { LocaleStore(androidContext()) }
    single { AppStrings(androidContext()) }
    single { GuideApiReachability() }
    single { UserLocationSource(androidContext()) }
    single { AiGuideSource(androidContext()) }
    single { RouteSummarySource(androidContext()) }
    singleOf(::AnonymousUserSource)
    singleOf(::PlacesApiSource)
    singleOf(::HistoricalPlacesApiSource)
    singleOf(::OverpassSource)
    singleOf(::OsrmSource)
    singleOf(::RouteApiSource)
    singleOf(::VoiceReadingSource)
    singleOf(::LocationRatingSource)
    singleOf(::PlaceDetailsSource)
    singleOf(::PlaceImageSource)
    singleOf(::PlaceStorySource)
    singleOf(::TourSource)

    singleOf(::PlacesRepositoryImpl) bind PlacesRepository::class
    singleOf(::HistoricalPlacesRepositoryImpl) bind HistoricalPlacesRepository::class
    singleOf(::OsrmRepositoryImpl) bind OsrmRepository::class
    singleOf(::UserLocationRepositoryImpl) bind UserLocationRepository::class
    singleOf(::PlaceDetailsRepositoryImpl) bind PlaceDetailsRepository::class
    singleOf(::AiGuideRepositoryImpl) bind AiGuideRepository::class
    singleOf(::TourRepositoryImpl) bind TourRepository::class
    singleOf(::AnonymousUserRepositoryImpl) bind AnonymousUserRepository::class
    singleOf(::RouteRepositoryImpl) bind RouteRepository::class
    singleOf(::VoiceReadingRepositoryImpl) bind VoiceReadingRepository::class
    singleOf(::LocationRatingRepositoryImpl) bind LocationRatingRepository::class
}

val domainModule = module {
    factoryOf(::GetNearbyPlacesUseCaseImpl) bind GetNearbyPlacesUseCase::class
    factoryOf(::GetHistoricalPlacesUseCaseImpl) bind GetHistoricalPlacesUseCase::class
    factoryOf(::GetHistoricalPlaceDetailsUseCaseImpl) bind GetHistoricalPlaceDetailsUseCase::class
    factoryOf(::GetUserLocationUseCaseImpl) bind GetUserLocationUseCase::class
    factoryOf(::GetPlaceDetailsUseCaseImpl) bind GetPlaceDetailsUseCase::class
    factoryOf(::GetAiGuideUseCaseImpl) bind GetAiGuideUseCase::class
    factoryOf(::ObserveAiGuidePlaybackUseCaseImpl) bind ObserveAiGuidePlaybackUseCase::class
    factoryOf(::ControlAiGuideUseCaseImpl) bind ControlAiGuideUseCase::class
    factoryOf(::GetTourProgressUseCaseImpl) bind GetTourProgressUseCase::class
    factoryOf(::GoToNextStopUseCaseImpl) bind GoToNextStopUseCase::class
    factoryOf(::GoToPreviousStopUseCaseImpl) bind GoToPreviousStopUseCase::class
    factoryOf(::MarkPlaceVisitedUseCaseImpl) bind MarkPlaceVisitedUseCase::class
    factoryOf(::ContinueRouteUseCaseImpl) bind ContinueRouteUseCase::class
    factoryOf(::EnsureAnonymousUserUseCaseImpl) bind EnsureAnonymousUserUseCase::class
    factoryOf(::BuildAdventureRouteUseCaseImpl) bind BuildAdventureRouteUseCase::class
    factoryOf(::RebuildAdventureRouteUseCaseImpl) bind RebuildAdventureRouteUseCase::class
    factoryOf(::GetOsrmRouteUseCaseImpl) bind GetOsrmRouteUseCase::class
    factoryOf(::OptimizeRoutePlacesUseCaseImpl) bind OptimizeRoutePlacesUseCase::class
    factoryOf(::RatePlaceUseCaseImpl) bind RatePlaceUseCase::class
}

val viewModelModule = module {
    viewModelOf(::LiveMapViewModel)
}

val appModules = listOf(dataModule, domainModule, viewModelModule)

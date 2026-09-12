package com.example.homework.di

import com.example.homework.core.data.repository.OverpassRepositoryImpl
import com.example.homework.core.data.repository.UserLocationRepositoryImpl
import com.example.homework.core.data.source.overpass.OverpassSource
import com.example.homework.core.data.source.userLocation.UserLocationSource
import com.example.homework.core.domain.repository.OverpassRepository
import com.example.homework.core.domain.repository.UserLocationRepository
import com.example.homework.core.domain.usecase.GetNearbyPlacesUseCase
import com.example.homework.core.domain.usecase.GetUserLocationUseCase
import com.example.homework.core.domain.usecase.impl.GetNearbyPlacesUseCaseImpl
import com.example.homework.core.domain.usecase.impl.GetUserLocationUseCaseImpl
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

    singleOf(::OverpassSource)
    single { UserLocationSource(androidContext()) }

    singleOf(::OverpassRepositoryImpl) bind OverpassRepository::class
    singleOf(::UserLocationRepositoryImpl) bind UserLocationRepository::class
}

val domainModule = module {
    factoryOf(::GetNearbyPlacesUseCaseImpl) bind GetNearbyPlacesUseCase::class
    factoryOf(::GetUserLocationUseCaseImpl) bind GetUserLocationUseCase::class
}

val viewModelModule = module {
    viewModelOf(::LiveMapViewModel)
}

val appModules = listOf(dataModule, domainModule, viewModelModule)

package com.example.homework

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.homework.core.locale.LocaleHelper
import com.example.homework.di.appModules
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.osmdroid.config.Configuration
import java.io.File

class HomeworkApp : Application(), ImageLoaderFactory {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@HomeworkApp)
            modules(appModules)
        }
        setupOsmDroid()
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .okHttpClient {
                OkHttpClient.Builder()
                    .addNetworkInterceptor { chain ->
                        chain.proceed(
                            chain.request().newBuilder()
                                .header("User-Agent", OSM_USER_AGENT)
                                .build(),
                        )
                    }
                    .build()
            }
            .crossfade(true)
            .build()

    private fun setupOsmDroid() {
        val prefs = getSharedPreferences("osmdroid", MODE_PRIVATE)
        val base = File(cacheDir, "osmdroid")
        if (prefs.getInt(TILE_CACHE_VERSION_KEY, 0) < TILE_CACHE_VERSION) {
            base.deleteRecursively()
            prefs.edit { putInt(TILE_CACHE_VERSION_KEY, TILE_CACHE_VERSION) }
        }
        base.mkdirs()
        Configuration.getInstance().apply {
            load(this@HomeworkApp, prefs)
            osmdroidBasePath = base
            osmdroidTileCache = File(base, "tiles").apply { mkdirs() }
            userAgentValue = OSM_USER_AGENT
            tileDownloadThreads = 2
        }
    }

    private companion object {
        const val TILE_CACHE_VERSION_KEY = "tile_cache_version"
        const val TILE_CACHE_VERSION = 3
    }
}

const val OSM_USER_AGENT =
    "KazanWalk/1.0 (com.example.homework; Android student homework; Overpass+OSM)"

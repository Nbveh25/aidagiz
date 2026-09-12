package com.example.homework

import android.app.Application
import com.example.homework.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.osmdroid.config.Configuration
import java.io.File
import androidx.core.content.edit

class HomeworkApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@HomeworkApp)
            modules(appModules)
        }
        setupOsmDroid()
    }

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

package com.example.homework.core.locale

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

object LocaleHelper {
    fun wrap(base: Context): Context {
        val language = LocaleStore(base).get()
        val locale = language.toLocale()
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLocales(LocaleList(locale))
        return base.createConfigurationContext(config)
    }
}

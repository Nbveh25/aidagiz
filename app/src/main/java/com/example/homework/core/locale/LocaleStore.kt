package com.example.homework.core.locale

import android.content.Context
import androidx.core.content.edit

class LocaleStore(context: Context) {
    private val prefs = (context.applicationContext ?: context)
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun get(): AppLanguage = AppLanguage.fromTag(prefs.getString(KEY_LANGUAGE, AppLanguage.Russian.tag))

    fun set(language: AppLanguage) {
        prefs.edit { putString(KEY_LANGUAGE, language.tag) }
    }

    private companion object {
        const val PREFS_NAME = "app_locale"
        const val KEY_LANGUAGE = "language"
    }
}

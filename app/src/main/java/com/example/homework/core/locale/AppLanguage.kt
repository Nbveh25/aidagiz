package com.example.homework.core.locale

import java.util.Locale

enum class AppLanguage(val tag: String) {
    Russian("ru"),
    Tatar("tt"),
    ;

    fun toLocale(): Locale = Locale.forLanguageTag(tag)

    companion object {
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.tag.equals(tag, ignoreCase = true) } ?: Russian
    }
}

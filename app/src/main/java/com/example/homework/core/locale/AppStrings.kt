package com.example.homework.core.locale

import android.content.Context
import androidx.annotation.StringRes

class AppStrings(
    private val context: Context,
) {
    fun get(@StringRes id: Int, vararg formatArgs: Any): String {
        val localized = LocaleHelper.wrap(context)
        return if (formatArgs.isEmpty()) {
            localized.getString(id)
        } else {
            localized.getString(id, *formatArgs)
        }
    }
}

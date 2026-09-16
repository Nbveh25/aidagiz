package com.example.homework.core.data.source.user

import android.content.Context
import androidx.core.content.edit

class UserIdStore(
    context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun get(): String? = prefs.getString(KEY_USER_ID, null)

    fun set(userId: String) {
        prefs.edit { putString(KEY_USER_ID, userId) }
    }

    private companion object {
        const val PREFS_NAME = "guide_api"
        const val KEY_USER_ID = "user_id"
    }
}

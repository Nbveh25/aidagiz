package com.example.homework.core.data.source.user

import com.example.homework.core.data.source.api.GuideApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AnonymousUserSource(
    private val api: GuideApiClient,
    private val userIdStore: UserIdStore,
) {
    suspend fun ensureUserId(): String = withContext(Dispatchers.IO) {
        userIdStore.get() ?: createUserId()
    }

    private fun createUserId(): String {
        val json = api.postJson("/users/anonymous", body = null)
        val userId = json.optString("userId")
        if (userId.isBlank()) {
            error("Сервер не вернул идентификатор пользователя")
        }
        userIdStore.set(userId)
        return userId
    }
}

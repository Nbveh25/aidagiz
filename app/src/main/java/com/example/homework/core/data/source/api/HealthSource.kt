package com.example.homework.core.data.source.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HealthSource(
    private val api: GuideApiClient,
) {
    suspend fun ping(): Boolean = withContext(Dispatchers.IO) {
        val json = api.getJson("/health")
        json.optString("status").equals("ok", ignoreCase = true) || json.length() == 0
    }
}

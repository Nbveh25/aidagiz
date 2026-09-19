package com.example.homework.core.data.source.api

/** Base URL сервиса TatarTouristGuideService из Swagger. */
object GuideApiConfig {
    const val BASE_URL = "http://10.2.67.189:5137"
    const val USER_ID_HEADER = "X-User-Id"

    fun rewriteMediaUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return url
            .replace("http://localhost:9000", "http://192.168.3.11:9000")
            .replace("http://127.0.0.1:9000", "http://192.168.3.11:9000")
            .replace("http://localhost", "http://192.168.3.11")
            .replace("http://127.0.0.1", "http://192.168.3.11")
    }
}

package com.example.homework.core.data.source.api

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Locale

class GuideApiClient(
    private val httpClient: OkHttpClient,
    private val baseUrl: String = GuideApiConfig.BASE_URL,
) {
    fun getJson(path: String, query: Map<String, String> = emptyMap()): JSONObject =
        getJson(
            pathSegments = path.trim('/').split('/').filter { it.isNotEmpty() },
            query = query,
        )

    fun getJson(pathSegments: List<String>, query: Map<String, String> = emptyMap()): JSONObject {
        val url = baseUrl.trimEnd('/').toHttpUrl().newBuilder().apply {
            pathSegments.forEach { addPathSegment(it) }
            query.forEach { (key, value) -> addQueryParameter(key, value) }
        }.build()
        return executeJson(
            Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .get()
                .build(),
        )
    }

    fun postJson(
        path: String,
        body: JSONObject?,
        headers: Map<String, String> = emptyMap(),
        accept: String = "application/json",
    ): JSONObject {
        val builder = Request.Builder()
            .url(urlBuilder(path).build())
            .header("Accept", accept)
        headers.forEach { (key, value) -> builder.header(key, value) }
        if (body == null) {
            builder.post(ByteArray(0).toRequestBody(null))
        } else {
            builder.header("Content-Type", JSON_MEDIA_TYPE.toString())
            builder.post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
        }
        return executeJson(builder.build())
    }

    fun putJson(
        path: String,
        body: JSONObject,
        headers: Map<String, String> = emptyMap(),
    ): JSONObject {
        val builder = Request.Builder()
            .url(urlBuilder(path).build())
            .header("Accept", "application/json")
            .header("Content-Type", JSON_MEDIA_TYPE.toString())
            .put(body.toString().toRequestBody(JSON_MEDIA_TYPE))
        headers.forEach { (key, value) -> builder.header(key, value) }
        return executeJson(builder.build())
    }

    fun postBytes(
        path: String,
        body: JSONObject,
        headers: Map<String, String> = emptyMap(),
        accept: String,
    ): ByteArray {
        val builder = Request.Builder()
            .url(urlBuilder(path).build())
            .header("Accept", accept)
            .header("Content-Type", JSON_MEDIA_TYPE.toString())
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
        headers.forEach { (key, value) -> builder.header(key, value) }
        httpClient.newCall(builder.build()).execute().use { response ->
            val bytes = response.body?.bytes() ?: ByteArray(0)
            if (!response.isSuccessful) {
                throw IOException(parseError(bytes.decodeToString(), response.code))
            }
            return bytes
        }
    }

    private fun executeJson(request: Request): JSONObject {
        httpClient.newCall(request).execute().use { response ->
            val payload = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException(parseError(payload, response.code))
            }
            if (payload.isBlank()) return JSONObject()
            return JSONObject(payload)
        }
    }

    private fun urlBuilder(path: String) =
        baseUrl.trimEnd('/').plus("/").plus(path.trimStart('/')).toHttpUrl().newBuilder()

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

fun parseError(payload: String, code: Int): String {
    if (payload.isBlank()) return "Ошибка $code"
    return runCatching {
        val json = JSONObject(payload)
        json.optString("message").takeIf { it.isNotBlank() }
            ?: json.optString("title").takeIf { it.isNotBlank() }
            ?: json.optJSONObject("errors")?.let { errors ->
                errors.keys().asSequence()
                    .flatMap { key ->
                        val value = errors.opt(key)
                        if (value is JSONArray) {
                            (0 until value.length()).map { value.optString(it) }
                        } else {
                            listOf(value.toString())
                        }
                    }
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                    .takeIf { it.isNotBlank() }
            }
            ?: "Ошибка $code"
    }.getOrElse { payload.take(180).ifBlank { "Ошибка $code" } }
}

fun JSONObject.stringOrNull(key: String): String? {
    if (!has(key) || isNull(key)) return null
    return optString(key).takeIf { it.isNotBlank() }
}

fun coordString(value: Double): String = String.format(Locale.US, "%.8f", value)

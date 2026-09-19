package com.example.homework.core.data.source.api

class GuideApiReachability {
    @Volatile
    private var downUntilMs: Long = 0

    fun isDown(): Boolean = System.currentTimeMillis() < downUntilMs

    fun markDown(durationMs: Long = DOWN_FOR_MS) {
        downUntilMs = System.currentTimeMillis() + durationMs
    }

    fun markUp() {
        downUntilMs = 0
    }

    private companion object {
        const val DOWN_FOR_MS = 60_000L
    }
}

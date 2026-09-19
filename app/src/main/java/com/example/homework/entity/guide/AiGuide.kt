package com.example.homework.entity.guide

data class AiGuideNarration(
    val placeId: String,
    val text: String,
    val durationMs: Long,
)

data class AiGuidePlayback(
    val isPlaying: Boolean = false,
    val isMuted: Boolean = false,
    val isReady: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 1L,
) {
    val progress: Float
        get() = if (durationMs == 0L) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)

    val positionLabel: String
        get() = formatClock(positionMs)

    val remainingLabel: String
        get() = "-${formatClock((durationMs - positionMs).coerceAtLeast(0))}"
}

private fun formatClock(ms: Long): String {
    val totalSeconds = (ms / 1000).toInt()
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

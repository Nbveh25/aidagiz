package com.example.homework.core.data.source.guide

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.example.homework.entity.guide.AiGuideNarration
import com.example.homework.entity.guide.AiGuidePlayback
import com.example.homework.entity.map.OsmPlace
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

class AiGuideSource(
    context: Context,
) {
    private val cacheDir = context.cacheDir
    private val playbackState = MutableStateFlow(AiGuidePlayback())
    private var player: MediaPlayer? = null
    val playback: StateFlow<AiGuidePlayback> = playbackState.asStateFlow()

    fun getNarration(place: OsmPlace): AiGuideNarration {
        return AiGuideNarration(
            placeId = place.id,
            text = place.description.trim(),
            durationMs = playbackState.value.durationMs,
        )
    }

    fun prepareAudio(bytes: ByteArray) {
        val file = File(cacheDir, AUDIO_FILE_NAME)
        file.writeBytes(bytes)
        releasePlayer()
        val mediaPlayer = MediaPlayer()
        try {
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build(),
            )
            mediaPlayer.setDataSource(file.absolutePath)
            mediaPlayer.setOnCompletionListener {
                playbackState.update { current ->
                    current.copy(isPlaying = false, isReady = true, positionMs = current.durationMs)
                }
            }
            mediaPlayer.prepare()
            val duration = mediaPlayer.duration.toLong().coerceAtLeast(1L)
            applyMute(mediaPlayer, playbackState.value.isMuted)
            player = mediaPlayer
            playbackState.update {
                it.copy(
                    isPlaying = false,
                    isReady = true,
                    positionMs = 0L,
                    durationMs = duration,
                )
            }
        } catch (error: Exception) {
            mediaPlayer.release()
            throw error
        }
    }

    fun hasPreparedAudio(): Boolean = player != null

    fun togglePlayback() {
        val mediaPlayer = player ?: return
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        } else {
            val nearEnd = mediaPlayer.duration > 0 &&
                mediaPlayer.currentPosition >= mediaPlayer.duration - 250
            if (nearEnd) mediaPlayer.seekTo(0)
            mediaPlayer.start()
        }
        playbackState.update {
            it.copy(
                isPlaying = mediaPlayer.isPlaying,
                isReady = true,
                positionMs = mediaPlayer.currentPosition.toLong(),
                durationMs = mediaPlayer.duration.toLong().coerceAtLeast(1L),
            )
        }
    }

    fun seek(progress: Float) {
        playbackState.update { current ->
            val position = (current.durationMs * progress.coerceIn(0f, 1f)).toLong()
            player?.seekTo(position.toInt())
            current.copy(positionMs = position)
        }
    }

    fun toggleMute() {
        playbackState.update { current ->
            val muted = !current.isMuted
            player?.let { applyMute(it, muted) }
            current.copy(isMuted = muted)
        }
    }

    fun tick(stepMs: Long = 500L) {
        val mediaPlayer = player ?: return
        if (!mediaPlayer.isPlaying) return
        playbackState.update {
            it.copy(
                isPlaying = true,
                isReady = true,
                positionMs = mediaPlayer.currentPosition.toLong(),
                durationMs = mediaPlayer.duration.toLong().coerceAtLeast(1L),
            )
        }
    }

    fun resetForPlace() {
        player?.let { mediaPlayer ->
            if (mediaPlayer.isPlaying) mediaPlayer.pause()
            mediaPlayer.seekTo(0)
        }
        playbackState.update {
            AiGuidePlayback(
                isMuted = it.isMuted,
                isReady = player != null,
                durationMs = player?.duration?.toLong()?.coerceAtLeast(1L) ?: 1L,
            )
        }
    }

    fun release() {
        releasePlayer()
        playbackState.value = AiGuidePlayback()
    }

    private fun releasePlayer() {
        player?.release()
        player = null
    }

    private fun applyMute(mediaPlayer: MediaPlayer, muted: Boolean) {
        val volume = if (muted) 0f else 1f
        mediaPlayer.setVolume(volume, volume)
    }

    private companion object {
        const val AUDIO_FILE_NAME = "speech-synthesize.wav"
    }
}

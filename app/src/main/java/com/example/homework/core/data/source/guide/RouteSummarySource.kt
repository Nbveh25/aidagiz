package com.example.homework.core.data.source.guide

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.example.homework.entity.guide.SpeechStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class RouteSummarySource(
    context: Context,
) {
    private val cacheDir = context.cacheDir
    private val statusState = MutableStateFlow(SpeechStatus.Idle)
    private val completedState = MutableStateFlow(false)
    private var player: MediaPlayer? = null
    private var preparedText: String? = null
    val status: StateFlow<SpeechStatus> = statusState.asStateFlow()
    val completed: StateFlow<Boolean> = completedState.asStateFlow()

    fun hasPrepared(text: String): Boolean =
        preparedText == text && player != null && statusState.value != SpeechStatus.Error

    fun setLoading() {
        statusState.value = SpeechStatus.Loading
    }

    fun setError() {
        releasePlayer()
        preparedText = null
        completedState.value = false
        statusState.value = SpeechStatus.Error
    }

    fun prepare(text: String, bytes: ByteArray) {
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
                completedState.value = true
                statusState.value = SpeechStatus.Ready
            }
            mediaPlayer.prepare()
            player = mediaPlayer
            preparedText = text
            completedState.value = false
            statusState.value = SpeechStatus.Ready
        } catch (error: Exception) {
            mediaPlayer.release()
            preparedText = null
            statusState.value = SpeechStatus.Error
            throw error
        }
    }

    fun togglePlayback() {
        val mediaPlayer = player ?: return
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            statusState.value = SpeechStatus.Paused
        } else {
            completedState.value = false
            mediaPlayer.start()
            statusState.value = SpeechStatus.Playing
        }
    }

    fun pause() {
        val mediaPlayer = player ?: return
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            statusState.value = SpeechStatus.Paused
        }
    }

    fun reset() {
        releasePlayer()
        preparedText = null
        completedState.value = false
        statusState.value = SpeechStatus.Idle
    }

    fun release() = reset()

    private fun releasePlayer() {
        player?.release()
        player = null
    }

    private companion object {
        const val AUDIO_FILE_NAME = "route-summary.wav"
    }
}

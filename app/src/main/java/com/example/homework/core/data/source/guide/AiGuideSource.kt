package com.example.homework.core.data.source.guide

import android.content.Context
import android.media.MediaPlayer
import com.example.homework.entity.guide.AiGuideNarration
import com.example.homework.entity.guide.AiGuidePlayback
import com.example.homework.entity.map.OsmPlace
import com.example.homework.entity.map.PlaceCategory
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

    fun getNarration(place: OsmPlace, textOverride: String? = null): AiGuideNarration {
        val text = textOverride?.takeIf { it.isNotBlank() }
            ?: place.description.ifBlank { narrationText(place) }
        return AiGuideNarration(
            placeId = place.id,
            text = text,
            durationMs = playbackState.value.durationMs,
        )
    }

    fun prepareAudio(bytes: ByteArray) {
        val file = File(cacheDir, AUDIO_FILE_NAME)
        file.writeBytes(bytes)
        releasePlayer()
        val mediaPlayer = MediaPlayer()
        mediaPlayer.setDataSource(file.absolutePath)
        mediaPlayer.setOnCompletionListener {
            playbackState.update { current ->
                current.copy(isPlaying = false, positionMs = current.durationMs)
            }
        }
        mediaPlayer.prepare()
        val duration = mediaPlayer.duration.toLong().coerceAtLeast(1L)
        applyMute(mediaPlayer, playbackState.value.isMuted)
        player = mediaPlayer
        playbackState.update {
            it.copy(
                isPlaying = false,
                positionMs = 0L,
                durationMs = duration,
            )
        }
    }

    fun togglePlayback() {
        val mediaPlayer = player
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
            } else {
                mediaPlayer.start()
            }
            playbackState.update {
                it.copy(
                    isPlaying = mediaPlayer.isPlaying,
                    positionMs = mediaPlayer.currentPosition.toLong(),
                    durationMs = mediaPlayer.duration.toLong().coerceAtLeast(1L),
                )
            }
        } else {
            playbackState.update { it.copy(isPlaying = !it.isPlaying) }
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
        val mediaPlayer = player
        if (mediaPlayer != null) {
            if (!mediaPlayer.isPlaying) return
            playbackState.update {
                it.copy(
                    isPlaying = true,
                    positionMs = mediaPlayer.currentPosition.toLong(),
                    durationMs = mediaPlayer.duration.toLong().coerceAtLeast(1L),
                )
            }
            return
        }
        playbackState.update { current ->
            if (!current.isPlaying) return@update current
            val next = (current.positionMs + stepMs).coerceAtMost(current.durationMs)
            current.copy(
                positionMs = next,
                isPlaying = next < current.durationMs,
            )
        }
    }

    fun resetForPlace() {
        player?.let { mediaPlayer ->
            if (mediaPlayer.isPlaying) mediaPlayer.pause()
            mediaPlayer.seekTo(0)
        }
        playbackState.update {
            AiGuidePlayback(isMuted = it.isMuted, durationMs = player?.duration?.toLong()?.coerceAtLeast(1L) ?: 1L)
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

    private fun narrationText(place: OsmPlace): String {
        val intro = when (place.category) {
            PlaceCategory.Mosque ->
                "Купола и минареты видны издалека. Во дворе тише, чем на улице: слышен ветер и шаги. " +
                    "Обратите внимание на орнамент портала и на то, как свет падает на стены ближе к закату."
            PlaceCategory.Temple ->
                "Фасад держит ритм старого города. Внутри обычно прохладнее, голоса приглушены. " +
                    "Стоит остановиться у входа и сравнить декор с соседними зданиями той же эпохи."
            PlaceCategory.Museum ->
                "Коллекция собрана так, чтобы за час сложилась картина города: быт, ремёсла, лица. " +
                    "Не гонитесь за всеми залами — выберите два сюжета и рассмотрите их спокойно."
            PlaceCategory.Theatre ->
                "Театр здесь не только сцена, но и площадь перед ним. " +
                    "Вечером фасад подсвечивают, днём можно обойти здание и посмотреть на детали карниза."
            PlaceCategory.Cafe ->
                "Место для паузы: чай, эчпочмак, короткая остановка между точками маршрута. " +
                    "Если шумно у окна, сядьте глубже в зал — там обычно тише."
            PlaceCategory.Restaurant ->
                "Локальная кухня — часть прогулки, не отдельная программа. " +
                    "Закажите одно татарское блюдо и не затягивайте обед, если впереди ещё есть точки."
            PlaceCategory.Park, PlaceCategory.Garden ->
                "Дорожки расходятся от центральной аллеи. Сверните с главного пути на минуту: " +
                    "скамейки в тени и вид на воду часто лучше, чем у входа."
            PlaceCategory.Historic ->
                "Камень и таблички хранят слой за слоем: ханская Казань, губернский город, советские годы. " +
                    "Прочитайте одну табличку вслух — маршрут сразу становится рассказом."
            PlaceCategory.Attraction, PlaceCategory.Artwork, PlaceCategory.ArtsCentre ->
                "Сюда приходят за кадром, но место живёт и без фото. " +
                    "Обойдите точку кругом: задний ракурс часто спокойнее и ближе к настоящему масштабу."
            PlaceCategory.Gallery ->
                "Залы лучше смотреть медленно: одна работа, одна пауза. " +
                    "Не пытайтесь обойти всё — выберите два сюжета."
            PlaceCategory.Viewpoint ->
                "Вид раскрывается не сразу: сделайте шаг в сторону от самой людной точки. " +
                    "Там линия горизонта спокойнее, а город читается целиком."
            PlaceCategory.Other ->
                "Точка не из парадного списка, зато рядом с маршрутом. " +
                    "Задержитесь на пару минут: такие места связывают известные остановки в цельный путь."
        }
        val name = place.name.ifBlank { "это место" }
        return "$name. $intro\n\nТак AI-гид помогает не торопиться: один взгляд, одна деталь, и можно идти дальше."
    }

    private companion object {
        const val AUDIO_FILE_NAME = "voice-reading.wav"
    }
}

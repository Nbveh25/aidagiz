package com.example.homework.core.domain.usecase

interface ControlAiGuideUseCase {
    fun togglePlayback()
    fun seek(progress: Float)
    fun toggleMute()
    fun resetForPlace()
    fun tick()
}

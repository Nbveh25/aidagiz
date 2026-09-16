package com.example.homework.core.domain.usecase

interface EnsureAnonymousUserUseCase {
    suspend operator fun invoke(): String
}

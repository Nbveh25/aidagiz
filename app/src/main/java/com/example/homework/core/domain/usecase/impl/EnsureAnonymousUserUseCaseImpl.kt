package com.example.homework.core.domain.usecase.impl

import com.example.homework.core.domain.repository.AnonymousUserRepository
import com.example.homework.core.domain.usecase.EnsureAnonymousUserUseCase

class EnsureAnonymousUserUseCaseImpl(
    private val anonymousUserRepository: AnonymousUserRepository,
) : EnsureAnonymousUserUseCase {
    override suspend fun invoke(): String = anonymousUserRepository.ensureUserId()
}

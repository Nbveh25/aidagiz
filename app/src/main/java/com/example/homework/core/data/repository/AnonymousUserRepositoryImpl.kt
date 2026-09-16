package com.example.homework.core.data.repository

import com.example.homework.core.data.source.user.AnonymousUserSource
import com.example.homework.core.domain.repository.AnonymousUserRepository

class AnonymousUserRepositoryImpl(
    private val anonymousUserSource: AnonymousUserSource,
) : AnonymousUserRepository {
    override suspend fun ensureUserId(): String = anonymousUserSource.ensureUserId()
}

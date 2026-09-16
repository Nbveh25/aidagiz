package com.example.homework.core.domain.repository

interface AnonymousUserRepository {
    suspend fun ensureUserId(): String
}

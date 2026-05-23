package com.quizifyai.domain.repository

import com.quizifyai.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: User?

    fun observeAuthState(): Flow<User?>

    suspend fun login(email: String, password: String): User

    suspend fun register(email: String, password: String): User

    suspend fun logout()
}

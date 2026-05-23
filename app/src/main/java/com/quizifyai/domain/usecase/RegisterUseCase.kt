package com.quizifyai.domain.usecase

import com.quizifyai.domain.repository.AuthRepository

class RegisterUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String) =
        authRepository.register(email.trim(), password)
}

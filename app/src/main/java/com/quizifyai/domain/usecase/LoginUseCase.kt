package com.quizifyai.domain.usecase

import com.quizifyai.domain.repository.AuthRepository

class LoginUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String) =
        authRepository.login(email.trim(), password)
}

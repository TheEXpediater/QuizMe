package com.quizifyai.domain.usecase

import com.quizifyai.domain.repository.AuthRepository

class ObserveAuthStateUseCase(private val authRepository: AuthRepository) {
    operator fun invoke() = authRepository.observeAuthState()
}

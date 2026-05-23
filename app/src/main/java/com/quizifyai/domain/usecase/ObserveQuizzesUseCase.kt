package com.quizifyai.domain.usecase

import com.quizifyai.domain.repository.AuthRepository
import com.quizifyai.domain.repository.QuizRepository

class ObserveQuizzesUseCase(
    private val authRepository: AuthRepository,
    private val quizRepository: QuizRepository,
) {
    operator fun invoke() =
        authRepository.currentUser?.let { quizRepository.observeQuizzes(it.id) }
}

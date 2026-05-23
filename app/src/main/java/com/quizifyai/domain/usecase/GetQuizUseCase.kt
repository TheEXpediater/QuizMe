package com.quizifyai.domain.usecase

import com.quizifyai.domain.repository.QuizRepository

class GetQuizUseCase(private val quizRepository: QuizRepository) {
    suspend operator fun invoke(quizId: String) = quizRepository.getQuiz(quizId)
}

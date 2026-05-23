package com.quizifyai.domain.repository

import com.quizifyai.domain.model.Quiz
import kotlinx.coroutines.flow.Flow

interface QuizRepository {
    fun observeQuizzes(userId: String, limit: Long = 25): Flow<List<Quiz>>

    suspend fun getQuiz(quizId: String): Quiz?

    suspend fun saveQuiz(userId: String, quiz: Quiz): Quiz
}

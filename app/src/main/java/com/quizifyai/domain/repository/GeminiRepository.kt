package com.quizifyai.domain.repository

import com.quizifyai.domain.model.Quiz

interface GeminiRepository {
    suspend fun generateQuiz(content: String, pdfName: String): Quiz
}

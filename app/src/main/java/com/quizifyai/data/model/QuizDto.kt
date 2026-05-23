package com.quizifyai.data.model

import androidx.annotation.Keep
import com.quizifyai.domain.model.Quiz

@Keep
data class QuizDto(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val questions: List<QuestionDto> = emptyList(),
    val createdAt: Long = 0L,
    val pdfName: String = "",
    val extractedText: String? = null,
)

fun QuizDto.toDomain() = Quiz(
    id = id,
    title = title,
    questions = questions.map { it.toDomain() },
    createdAt = createdAt,
    pdfName = pdfName,
)

fun Quiz.toDto(userId: String) = QuizDto(
    id = id,
    userId = userId,
    title = title,
    questions = questions.map { it.toDto() },
    createdAt = createdAt,
    pdfName = pdfName,
)

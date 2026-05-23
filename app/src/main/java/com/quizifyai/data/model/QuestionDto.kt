package com.quizifyai.data.model

import androidx.annotation.Keep
import com.quizifyai.domain.model.Question

@Keep
data class QuestionDto(
    val question: String = "",
    val choices: List<String> = emptyList(),
    val correctAnswer: String = "",
    val explanation: String = "",
)

fun QuestionDto.toDomain() = Question(
    question = question,
    choices = choices,
    correctAnswer = correctAnswer,
    explanation = explanation,
)

fun Question.toDto() = QuestionDto(
    question = question,
    choices = choices,
    correctAnswer = correctAnswer,
    explanation = explanation,
)

package com.quizifyai.domain.model

data class Question(
    val question: String,
    val choices: List<String>,
    val correctAnswer: String,
    val explanation: String,
)

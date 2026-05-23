package com.quizifyai.domain.model

data class Quiz(
    val id: String,
    val title: String,
    val questions: List<Question>,
    val createdAt: Long,
    val pdfName: String,
)

package com.quizifyai.domain.model

sealed class QuizifyException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NotAuthenticated : QuizifyException("Please sign in again.")
    class InvalidPdf : QuizifyException("Please choose a valid PDF file.")
    class EmptyPdfText : QuizifyException("No readable text was found in this PDF.")
    class MissingGeminiKey : QuizifyException("Gemini API key is missing. Add GEMINI_API_KEY to local.properties.")
    class ProcessingFailed(cause: Throwable? = null) : QuizifyException("PDF processing failed. Please try again.", cause)
    class GeminiFailed(cause: Throwable? = null) : QuizifyException("Quiz generation failed. Please try again.", cause)
    class NetworkIssue(cause: Throwable? = null) : QuizifyException("Network error. Check your connection and try again.", cause)
    class Unknown(cause: Throwable? = null) : QuizifyException("Something went wrong. Please try again.", cause)
}

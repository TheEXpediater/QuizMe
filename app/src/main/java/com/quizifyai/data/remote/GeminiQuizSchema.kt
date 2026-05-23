package com.quizifyai.data.remote

object GeminiQuizSchema {
    fun quizSchema(): GeminiSchema {
        val questionSchema = GeminiSchema(
            type = "OBJECT",
            properties = mapOf(
                "question" to GeminiSchema(type = "STRING"),
                "choices" to GeminiSchema(
                    type = "ARRAY",
                    items = GeminiSchema(type = "STRING"),
                ),
                "correctAnswer" to GeminiSchema(type = "STRING"),
                "explanation" to GeminiSchema(type = "STRING"),
            ),
            required = listOf("question", "choices", "correctAnswer", "explanation"),
        )

        return GeminiSchema(
            type = "OBJECT",
            properties = mapOf(
                "title" to GeminiSchema(type = "STRING"),
                "questions" to GeminiSchema(
                    type = "ARRAY",
                    items = questionSchema,
                ),
            ),
            required = listOf("title", "questions"),
        )
    }
}

package com.quizifyai.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig,
)

@Serializable
data class GeminiContent(
    val parts: List<GeminiPart> = emptyList(),
)

@Serializable
data class GeminiPart(
    val text: String = "",
)

@Serializable
data class GeminiGenerationConfig(
    val temperature: Double = 0.2,
    val maxOutputTokens: Int = 4096,
    val responseMimeType: String = "application/json",
    val responseSchema: GeminiSchema,
)

@Serializable
data class GeminiSchema(
    val type: String,
    val properties: Map<String, GeminiSchema>? = null,
    val required: List<String>? = null,
    val items: GeminiSchema? = null,
)

@Serializable
data class GeminiResponse(
    val candidates: List<GeminiCandidate> = emptyList(),
)

@Serializable
data class GeminiCandidate(
    val content: GeminiContent? = null,
)

@Serializable
data class GeminiQuizPayload(
    val title: String,
    val questions: List<GeminiQuestionPayload>,
)

@Serializable
data class GeminiQuestionPayload(
    val question: String,
    val choices: List<String>,
    val correctAnswer: String,
    val explanation: String,
)

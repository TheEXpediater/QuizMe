package com.quizifyai.data.repository

import com.quizifyai.data.remote.GeminiContent
import com.quizifyai.data.remote.GeminiGenerationConfig
import com.quizifyai.data.remote.GeminiPart
import com.quizifyai.data.remote.GeminiQuestionPayload
import com.quizifyai.data.remote.GeminiQuizPayload
import com.quizifyai.data.remote.GeminiQuizSchema
import com.quizifyai.data.remote.GeminiRequest
import com.quizifyai.data.remote.GeminiService
import com.quizifyai.domain.model.Question
import com.quizifyai.domain.model.Quiz
import com.quizifyai.domain.model.QuizifyException
import com.quizifyai.domain.repository.GeminiRepository
import com.quizifyai.utils.AiJsonSanitizer
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

class GeminiRepositoryImpl(
    private val service: GeminiService,
    private val apiKey: String,
    private val model: String,
) : GeminiRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun generateQuiz(content: String, pdfName: String): Quiz {
        if (apiKey.isBlank()) throw QuizifyException.MissingGeminiKey()

        return try {
            val response = service.generateContent(
                model = model,
                apiKey = apiKey,
                request = GeminiRequest(
                    contents = listOf(GeminiContent(parts = listOf(GeminiPart(promptFor(content))))),
                    generationConfig = GeminiGenerationConfig(
                        responseSchema = GeminiQuizSchema.quizSchema(),
                    ),
                ),
            )

            val text = response.candidates
                .firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                .orEmpty()

            parseQuiz(text, pdfName)
        } catch (error: QuizifyException) {
            throw error
        } catch (error: IOException) {
            throw QuizifyException.NetworkIssue(error)
        } catch (error: HttpException) {
            throw QuizifyException.GeminiFailed(error)
        } catch (error: SerializationException) {
            throw QuizifyException.GeminiFailed(error)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            throw QuizifyException.GeminiFailed(error)
        }
    }

    private fun promptFor(content: String): String {
        val safeContent = content
            .take(MAX_INPUT_CHARS)
            .replace(controlCharactersRegex, " ")

        return """
            Generate exactly 10 multiple choice questions from this academic content.
            Include a concise title for the quiz.
            Each question must include:
            - question
            - 4 choices
            - correctAnswer
            - explanation

            Return JSON only. Do not include markdown.

            Academic content:
            $safeContent
        """.trimIndent()
    }

    private fun parseQuiz(rawText: String, pdfName: String): Quiz {
        val payload = json.decodeFromString<GeminiQuizPayload>(
            AiJsonSanitizer.extractJsonObject(rawText),
        )

        val questions = payload.questions
            .mapNotNull { it.toQuestionOrNull() }
            .take(10)

        if (questions.isEmpty()) throw QuizifyException.GeminiFailed()

        return Quiz(
            id = "",
            title = payload.title.trim().ifBlank { pdfName.removeSuffix(".pdf") }.take(MAX_TITLE_CHARS),
            questions = questions,
            createdAt = System.currentTimeMillis(),
            pdfName = pdfName,
        )
    }

    private fun GeminiQuestionPayload.toQuestionOrNull(): Question? {
        val cleanChoices = choices
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .take(4)

        if (question.isBlank() || cleanChoices.size != 4) return null

        return Question(
            question = question.trim(),
            choices = cleanChoices,
            correctAnswer = normalizeAnswer(correctAnswer, cleanChoices),
            explanation = explanation.trim().ifBlank { "Review the source material for this answer." },
        )
    }

    private fun normalizeAnswer(answer: String, choices: List<String>): String {
        val trimmed = answer.trim()
        choices.firstOrNull { it.equals(trimmed, ignoreCase = true) }?.let { return it }

        val letterIndex = when (trimmed.removeSuffix(".").uppercase()) {
            "A" -> 0
            "B" -> 1
            "C" -> 2
            "D" -> 3
            else -> -1
        }

        return choices.getOrNull(letterIndex) ?: trimmed.ifBlank { choices.first() }
    }

    private companion object {
        const val MAX_INPUT_CHARS = 18_000
        const val MAX_TITLE_CHARS = 80
        val controlCharactersRegex = Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]")
    }
}

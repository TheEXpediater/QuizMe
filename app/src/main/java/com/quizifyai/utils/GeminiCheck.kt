package com.quizifyai.utils

import android.util.Log
import com.quizifyai.BuildConfig
import com.quizifyai.data.remote.GeminiClient
import com.quizifyai.data.remote.GeminiContent
import com.quizifyai.data.remote.GeminiGenerationConfig
import com.quizifyai.data.remote.GeminiPart
import com.quizifyai.data.remote.GeminiRequest
import com.quizifyai.data.remote.GeminiSchema
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiCheck {
    private const val TAG = "GeminiCheck"

    suspend fun runCheck(): Boolean {
        return withContext(Dispatchers.IO) {
            if (BuildConfig.GEMINI_API_KEY.isBlank()) {
                Log.w(TAG, "Gemini API key is empty; skipping network check")
                return@withContext false
            }

            try {
                val service = GeminiClient.create()
                val response = service.generateContent(
                    model = BuildConfig.GEMINI_MODEL,
                    apiKey = BuildConfig.GEMINI_API_KEY,
                    request = GeminiRequest(
                        contents = listOf(
                            GeminiContent(
                                parts = listOf(
                                    GeminiPart(text = "Reply with a short confirmation message."),
                                ),
                            ),
                        ),
                        generationConfig = GeminiGenerationConfig(
                            temperature = 0.2,
                            maxOutputTokens = 64,
                            responseMimeType = "text/plain",
                            responseSchema = GeminiSchema(type = "STRING"),
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

                if (text.isNotBlank()) {
                    Log.d(TAG, "Gemini check succeeded: $text")
                    true
                } else {
                    Log.w(TAG, "Gemini check returned an empty response")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gemini check failed", e)
                false
            }
        }
    }
}

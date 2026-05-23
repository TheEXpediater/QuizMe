package com.quizifyai.utils

object AiJsonSanitizer {
    fun extractJsonObject(raw: String): String {
        val withoutFence = raw
            .replace("```json", "", ignoreCase = true)
            .replace("```", "")
            .trim()

        val start = withoutFence.indexOf('{')
        if (start == -1) return withoutFence

        var depth = 0
        var inString = false
        var escaped = false

        for (index in start until withoutFence.length) {
            val char = withoutFence[index]
            when {
                escaped -> escaped = false
                char == '\\' && inString -> escaped = true
                char == '"' -> inString = !inString
                !inString && char == '{' -> depth++
                !inString && char == '}' -> {
                    depth--
                    if (depth == 0) {
                        return withoutFence.substring(start, index + 1)
                    }
                }
            }
        }

        return withoutFence.substring(start)
    }
}

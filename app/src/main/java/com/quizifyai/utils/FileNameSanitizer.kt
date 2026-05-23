package com.quizifyai.utils

object FileNameSanitizer {
    private val unsafeCharacters = Regex("[^A-Za-z0-9._-]")

    fun sanitizePdfName(name: String): String {
        val baseName = name
            .substringAfterLast('/')
            .ifBlank { "academic-file.pdf" }
            .replace(unsafeCharacters, "_")
            .take(80)
            .trim('_', '.', '-')
            .ifBlank { "academic-file" }

        return if (baseName.endsWith(".pdf", ignoreCase = true)) {
            baseName
        } else {
            "$baseName.pdf"
        }
    }
}

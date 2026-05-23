package com.quizifyai.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.quizifyai.domain.model.QuizifyException
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SimplePdfTextExtractor(private val context: Context) {
    private val TAG = "PdfExtractor"

    init {
        PDFBoxResourceLoader.init(context)
    }

    suspend fun extract(uri: Uri): String = withContext(Dispatchers.IO) {
        Log.d(TAG, "Extracting text from URI using PdfBox: $uri")
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw QuizifyException.InvalidPdf()
            
            inputStream.use { stream ->
                PDDocument.load(stream).use { document ->
                    val stripper = PDFTextStripper()
                    val text = stripper.getText(document)
                        .replace(Regex("\\s+"), " ")
                        .trim()
                    
                    Log.d(TAG, "Extracted ${text.length} characters")
                    if (text.isBlank()) throw QuizifyException.EmptyPdfText()
                    
                    text.take(MAX_TEXT_CHARS)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Extraction failed", e)
            if (e is QuizifyException) throw e
            throw QuizifyException.InvalidPdf()
        }
    }

    fun resolveFileName(uri: Uri): String {
        val displayName = if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
                }
        } else {
            null
        }

        val rawName = displayName ?: uri.lastPathSegment ?: "academic-file.pdf"
        return FileNameSanitizer.sanitizePdfName(rawName)
    }

    private companion object {
        const val MAX_TEXT_CHARS = 18_000
    }
}

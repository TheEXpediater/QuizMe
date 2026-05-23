package com.quizifyai.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.quizifyai.domain.model.ExtractedPdf
import com.quizifyai.domain.model.QuizifyException
import com.quizifyai.domain.model.UploadedPdf
import com.quizifyai.domain.repository.StorageRepository
import com.quizifyai.utils.FileNameSanitizer
import com.quizifyai.utils.SimplePdfTextExtractor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.io.IOException

class StorageRepositoryImpl(
    private val context: Context,
    private val storage: FirebaseStorage,
    private val pdfTextExtractor: SimplePdfTextExtractor,
) : StorageRepository {
    override suspend fun readPdf(uri: Uri): ExtractedPdf {
        val fileName = resolvePdfName(uri)
        val text = pdfTextExtractor.extract(uri)

        if (text.isBlank()) throw QuizifyException.EmptyPdfText()

        return ExtractedPdf(
            fileName = fileName,
            text = text,
        )
    }

    override suspend fun uploadPdf(
        userId: String,
        uri: Uri,
        fileName: String,
        generateOnServer: Boolean,
    ): UploadedPdf {
        try {
            val safeName = FileNameSanitizer.sanitizePdfName(fileName)
            ensurePdf(uri, safeName)

            val storageName = "${System.currentTimeMillis()}_$safeName"
            val reference = storage.reference.child("pdfs/$userId/$storageName")
            val metadata = StorageMetadata.Builder()
                .setContentType(PDF_MIME_TYPE)
                .setCustomMetadata("uploadedBy", userId)
                .setCustomMetadata("generateQuiz", generateOnServer.toString())
                .build()

            reference.putFile(uri, metadata).await()
            return UploadedPdf(fileName = safeName, storagePath = reference.path)
        } catch (error: QuizifyException) {
            throw error
        } catch (error: FirebaseNetworkException) {
            throw QuizifyException.NetworkIssue(error)
        } catch (error: IOException) {
            throw QuizifyException.NetworkIssue(error)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            throw QuizifyException.UploadFailed(error)
        }
    }

    private fun resolvePdfName(uri: Uri): String {
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
        ensurePdf(uri, rawName)
        return FileNameSanitizer.sanitizePdfName(rawName)
    }

    private fun ensurePdf(uri: Uri, fileName: String) {
        val mimeType = context.contentResolver.getType(uri)
        val isPdfMime = mimeType == PDF_MIME_TYPE
        val isPdfName = fileName.endsWith(".pdf", ignoreCase = true)

        if (!isPdfMime && !isPdfName) {
            throw QuizifyException.InvalidPdf()
        }
    }

    private companion object {
        const val PDF_MIME_TYPE = "application/pdf"
    }
}

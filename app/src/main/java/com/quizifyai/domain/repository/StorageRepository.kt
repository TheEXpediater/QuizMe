package com.quizifyai.domain.repository

import android.net.Uri
import com.quizifyai.domain.model.ExtractedPdf
import com.quizifyai.domain.model.UploadedPdf

interface StorageRepository {
    suspend fun readPdf(uri: Uri): ExtractedPdf

    suspend fun uploadPdf(
        userId: String,
        uri: Uri,
        fileName: String,
        generateOnServer: Boolean = false,
    ): UploadedPdf
}

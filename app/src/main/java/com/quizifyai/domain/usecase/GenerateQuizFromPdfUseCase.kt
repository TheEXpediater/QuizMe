package com.quizifyai.domain.usecase

import android.net.Uri
import com.quizifyai.domain.model.QuizifyException
import com.quizifyai.domain.repository.AuthRepository
import com.quizifyai.domain.repository.GeminiRepository
import com.quizifyai.domain.repository.QuizRepository
import com.quizifyai.domain.repository.StorageRepository
import kotlinx.coroutines.CancellationException

class GenerateQuizFromPdfUseCase(
    private val authRepository: AuthRepository,
    private val storageRepository: StorageRepository,
    private val geminiRepository: GeminiRepository,
    private val quizRepository: QuizRepository,
) {
    suspend operator fun invoke(uri: Uri) = try {
        val user = authRepository.currentUser ?: throw QuizifyException.NotAuthenticated()
        val extractedPdf = storageRepository.readPdf(uri)
        val uploadedPdf = storageRepository.uploadPdf(
            userId = user.id,
            uri = uri,
            fileName = extractedPdf.fileName,
            generateOnServer = false,
        )
        val generatedQuiz = geminiRepository.generateQuiz(
            content = extractedPdf.text,
            pdfName = uploadedPdf.fileName,
        )
        quizRepository.saveQuiz(user.id, generatedQuiz.copy(pdfName = uploadedPdf.fileName))
    } catch (error: CancellationException) {
        throw error
    } catch (error: QuizifyException) {
        throw error
    } catch (error: Exception) {
        throw QuizifyException.Unknown(error)
    }
}

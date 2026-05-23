package com.quizifyai.domain.usecase

import android.net.Uri
import com.quizifyai.domain.model.QuizifyException
import com.quizifyai.domain.repository.AuthRepository
import com.quizifyai.domain.repository.GeminiRepository
import com.quizifyai.domain.repository.QuizRepository
import com.quizifyai.utils.SimplePdfTextExtractor
import kotlinx.coroutines.CancellationException

class GenerateQuizFromPdfUseCase(
    private val authRepository: AuthRepository,
    private val pdfTextExtractor: SimplePdfTextExtractor,
    private val geminiRepository: GeminiRepository,
    private val quizRepository: QuizRepository,
) {
    suspend operator fun invoke(uri: Uri) = try {
        val user = authRepository.currentUser ?: throw QuizifyException.NotAuthenticated()
        
        // Extract text locally (No Storage upload)
        val text = try {
            pdfTextExtractor.extract(uri)
        } catch (e: QuizifyException) {
            throw e
        } catch (e: Exception) {
            throw QuizifyException.ProcessingFailed(e)
        }
        
        val fileName = pdfTextExtractor.resolveFileName(uri)

        // Generate quiz via Gemini
        val generatedQuiz = try {
            geminiRepository.generateQuiz(
                content = text,
                pdfName = fileName,
            )
        } catch (e: QuizifyException) {
            throw e
        } catch (e: Exception) {
            throw QuizifyException.GeminiFailed(e)
        }

        // Save to Firestore (only text and quiz data)
        quizRepository.saveQuiz(
            user.id,
            generatedQuiz.copy(pdfName = fileName)
        )
    } catch (error: CancellationException) {
        throw error
    } catch (error: QuizifyException) {
        throw error
    } catch (error: Exception) {
        throw QuizifyException.Unknown(error)
    }
}

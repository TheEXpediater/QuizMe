package com.quizifyai.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.quizifyai.data.model.QuizDto
import com.quizifyai.data.model.toDomain
import com.quizifyai.data.model.toDto
import com.quizifyai.domain.model.Quiz
import com.quizifyai.domain.repository.QuizRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class QuizRepositoryImpl(
    private val firestore: FirebaseFirestore,
) : QuizRepository {
    override fun observeQuizzes(userId: String, limit: Long): Flow<List<Quiz>> = callbackFlow {
        val registration = firestore.collection("quizzes")
            .whereEqualTo("userId", userId)
            .limit(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val quizzes = snapshot?.documents
                    ?.mapNotNull { document ->
                        document.toObject(QuizDto::class.java)
                            ?.copy(id = document.id)
                            ?.toDomain()
                    }
                    ?.sortedByDescending { it.createdAt }
                    .orEmpty()

                trySend(quizzes)
            }

        awaitClose { registration.remove() }
    }

    override suspend fun getQuiz(quizId: String): Quiz? {
        return firestore.collection("quizzes")
            .document(quizId)
            .get()
            .await()
            .toObject(QuizDto::class.java)
            ?.copy(id = quizId)
            ?.toDomain()
    }

    override suspend fun saveQuiz(userId: String, quiz: Quiz): Quiz {
        val document = if (quiz.id.isBlank()) {
            firestore.collection("quizzes").document()
        } else {
            firestore.collection("quizzes").document(quiz.id)
        }

        val savedQuiz = quiz.copy(
            id = document.id,
            createdAt = quiz.createdAt.takeIf { it > 0L } ?: System.currentTimeMillis(),
        )

        document.set(savedQuiz.toDto(userId)).await()

        firestore.collection("history")
            .add(
                mapOf(
                    "userId" to userId,
                    "quizId" to savedQuiz.id,
                    "pdfName" to savedQuiz.pdfName,
                    "type" to "quiz_generated",
                    "createdAt" to savedQuiz.createdAt,
                ),
            )
            .await()

        return savedQuiz
    }
}

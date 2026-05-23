package com.quizifyai.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.quizifyai.data.model.UserDto
import com.quizifyai.domain.model.User
import com.quizifyai.domain.repository.AuthRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : AuthRepository {
    override val currentUser: User?
        get() = auth.currentUser?.toDomainUser()

    override fun observeAuthState(): Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.toDomainUser())
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun login(email: String, password: String): User {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val firebaseUser = result.user ?: error("Firebase returned an empty user.")
        val user = firebaseUser.toDomainUser()
        ensureUserDocument(user)
        return user
    }

    override suspend fun register(email: String, password: String): User {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val firebaseUser = result.user ?: error("Firebase returned an empty user.")
        val user = firebaseUser.toDomainUser()
        ensureUserDocument(user)
        return user
    }

    override suspend fun logout() {
        auth.signOut()
    }

    private suspend fun ensureUserDocument(user: User) {
        firestore.collection("users")
            .document(user.id)
            .set(UserDto(id = user.id, email = user.email, createdAt = user.createdAt))
            .await()
    }

    private fun FirebaseUser.toDomainUser() = User(
        id = uid,
        email = email.orEmpty(),
        createdAt = metadata?.creationTimestamp ?: System.currentTimeMillis(),
    )
}

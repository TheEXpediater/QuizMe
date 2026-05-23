package com.quizifyai.utils

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.quizifyai.domain.model.QuizifyException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ErrorMessageMapper {
    fun toMessage(error: Throwable): String {
        return when (error) {
            is QuizifyException -> error.message.orEmpty()
            is FirebaseAuthWeakPasswordException -> "Use at least 6 characters for your password."
            is FirebaseAuthUserCollisionException -> "An account already exists with this email."
            is FirebaseAuthInvalidCredentialsException -> "Check your email and password, then try again."
            is FirebaseNetworkException,
            is UnknownHostException,
            is SocketTimeoutException,
            is IOException,
            -> "Network error. Check your connection and try again."
            else -> "Something went wrong. Please try again."
        }
    }
}

package com.quizifyai.utils

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.quizifyai.BuildConfig
import com.quizifyai.data.remote.GeminiClient
import com.quizifyai.data.repository.AuthRepositoryImpl
import com.quizifyai.data.repository.GeminiRepositoryImpl
import com.quizifyai.data.repository.QuizRepositoryImpl
import com.quizifyai.data.repository.StorageRepositoryImpl
import com.quizifyai.domain.repository.AuthRepository
import com.quizifyai.domain.repository.GeminiRepository
import com.quizifyai.domain.repository.QuizRepository
import com.quizifyai.domain.repository.StorageRepository
import com.quizifyai.domain.usecase.GenerateQuizFromPdfUseCase
import com.quizifyai.domain.usecase.GetQuizUseCase
import com.quizifyai.domain.usecase.LoginUseCase
import com.quizifyai.domain.usecase.LogoutUseCase
import com.quizifyai.domain.usecase.ObserveAuthStateUseCase
import com.quizifyai.domain.usecase.ObserveQuizzesUseCase
import com.quizifyai.domain.usecase.RegisterUseCase
import com.quizifyai.presentation.viewmodel.AuthViewModel
import com.quizifyai.presentation.viewmodel.HomeViewModel
import com.quizifyai.presentation.viewmodel.QuizDetailViewModel

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    init {
        if (FirebaseApp.getApps(appContext).isEmpty()) {
            FirebaseApp.initializeApp(appContext)
        }
    }

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }

    private val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(auth, firestore)
    }

    private val quizRepository: QuizRepository by lazy {
        QuizRepositoryImpl(firestore)
    }

    private val storageRepository: StorageRepository by lazy {
        StorageRepositoryImpl(
            context = appContext,
            storage = storage,
            pdfTextExtractor = SimplePdfTextExtractor(appContext),
        )
    }

    private val geminiRepository: GeminiRepository by lazy {
        GeminiRepositoryImpl(
            service = GeminiClient.create(),
            apiKey = BuildConfig.GEMINI_API_KEY,
            model = BuildConfig.GEMINI_MODEL,
        )
    }

    fun authViewModelFactory(): ViewModelProvider.Factory = simpleFactory {
        AuthViewModel(
            loginUseCase = LoginUseCase(authRepository),
            registerUseCase = RegisterUseCase(authRepository),
            logoutUseCase = LogoutUseCase(authRepository),
            observeAuthStateUseCase = ObserveAuthStateUseCase(authRepository),
        )
    }

    fun homeViewModelFactory(): ViewModelProvider.Factory = simpleFactory {
        HomeViewModel(
            observeQuizzesUseCase = ObserveQuizzesUseCase(authRepository, quizRepository),
            generateQuizFromPdfUseCase = GenerateQuizFromPdfUseCase(
                authRepository = authRepository,
                storageRepository = storageRepository,
                geminiRepository = geminiRepository,
                quizRepository = quizRepository,
            ),
        )
    }

    fun quizDetailViewModelFactory(): ViewModelProvider.Factory = simpleFactory {
        QuizDetailViewModel(
            getQuizUseCase = GetQuizUseCase(quizRepository),
        )
    }

    private inline fun <reified T : ViewModel> simpleFactory(crossinline create: () -> T) =
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <VM : ViewModel> create(modelClass: Class<VM>): VM {
                if (modelClass.isAssignableFrom(T::class.java)) {
                    return create() as VM
                }
                error("Unknown ViewModel class: ${modelClass.name}")
            }
        }
}

package com.quizifyai.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizifyai.domain.model.Quiz
import com.quizifyai.domain.usecase.GenerateQuizFromPdfUseCase
import com.quizifyai.domain.usecase.ObserveQuizzesUseCase
import com.quizifyai.utils.ErrorMessageMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val quizzes: List<Quiz> = emptyList(),
    val isLoadingQuizzes: Boolean = true,
    val isGenerating: Boolean = false,
    val message: String? = null,
    val errorMessage: String? = null,
)

class HomeViewModel(
    private val observeQuizzesUseCase: ObserveQuizzesUseCase,
    private val generateQuizFromPdfUseCase: GenerateQuizFromPdfUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeQuizzes()
    }

    fun onPdfSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGenerating = true,
                    message = "Uploading PDF and generating quiz...",
                    errorMessage = null,
                )
            }

            runCatching { generateQuizFromPdfUseCase(uri) }
                .onSuccess { quiz ->
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            message = "Quiz ready: ${quiz.title}",
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            message = null,
                            errorMessage = ErrorMessageMapper.toMessage(error),
                        )
                    }
                }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun observeQuizzes() {
        val quizFlow = observeQuizzesUseCase()
        if (quizFlow == null) {
            _uiState.update {
                it.copy(
                    isLoadingQuizzes = false,
                    errorMessage = "Please sign in again.",
                )
            }
            return
        }

        viewModelScope.launch {
            quizFlow
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingQuizzes = false,
                            errorMessage = ErrorMessageMapper.toMessage(error),
                        )
                    }
                }
                .collect { quizzes ->
                    _uiState.update {
                        it.copy(
                            quizzes = quizzes,
                            isLoadingQuizzes = false,
                        )
                    }
                }
        }
    }
}

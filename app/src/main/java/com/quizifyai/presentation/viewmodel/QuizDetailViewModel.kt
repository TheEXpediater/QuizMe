package com.quizifyai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizifyai.domain.model.Quiz
import com.quizifyai.domain.usecase.GetQuizUseCase
import com.quizifyai.utils.ErrorMessageMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface QuizDetailUiState {
    data object Loading : QuizDetailUiState
    data class Loaded(val quiz: Quiz) : QuizDetailUiState
    data class Error(val message: String) : QuizDetailUiState
}

class QuizDetailViewModel(
    private val getQuizUseCase: GetQuizUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<QuizDetailUiState>(QuizDetailUiState.Loading)
    val uiState: StateFlow<QuizDetailUiState> = _uiState.asStateFlow()

    fun loadQuiz(quizId: String) {
        viewModelScope.launch {
            _uiState.value = QuizDetailUiState.Loading
            runCatching { getQuizUseCase(quizId) }
                .onSuccess { quiz ->
                    _uiState.value = if (quiz == null) {
                        QuizDetailUiState.Error("Quiz not found.")
                    } else {
                        QuizDetailUiState.Loaded(quiz)
                    }
                }
                .onFailure { error ->
                    _uiState.value = QuizDetailUiState.Error(ErrorMessageMapper.toMessage(error))
                }
        }
    }
}

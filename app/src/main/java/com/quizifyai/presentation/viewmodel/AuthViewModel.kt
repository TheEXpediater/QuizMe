package com.quizifyai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizifyai.domain.model.User
import com.quizifyai.domain.usecase.LoginUseCase
import com.quizifyai.domain.usecase.LogoutUseCase
import com.quizifyai.domain.usecase.ObserveAuthStateUseCase
import com.quizifyai.domain.usecase.RegisterUseCase
import com.quizifyai.utils.ErrorMessageMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val currentUser: User? = null,
    val isCheckingAuth: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class AuthViewModel(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val logoutUseCase: LogoutUseCase,
    observeAuthStateUseCase: ObserveAuthStateUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeAuthStateUseCase().collect { user ->
                _uiState.update {
                    it.copy(
                        currentUser = user,
                        isCheckingAuth = false,
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun login(email: String, password: String) {
        if (!validate(email, password)) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { loginUseCase(email, password) }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = ErrorMessageMapper.toMessage(error))
                    }
                }
        }
    }

    fun register(email: String, password: String) {
        if (!validate(email, password)) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { registerUseCase(email, password) }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = ErrorMessageMapper.toMessage(error))
                    }
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun validate(email: String, password: String): Boolean {
        val message = when {
            email.isBlank() -> "Enter your email address."
            !email.contains("@") -> "Enter a valid email address."
            password.length < 6 -> "Password must be at least 6 characters."
            else -> null
        }

        if (message != null) {
            _uiState.update { it.copy(errorMessage = message) }
            return false
        }

        return true
    }
}

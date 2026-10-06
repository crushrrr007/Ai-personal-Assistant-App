package com.example.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repo.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthMode {
    SIGN_IN,
    SIGN_UP
}

data class AuthUiState(
    val mode: AuthMode = AuthMode.SIGN_IN,
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun setMode(mode: AuthMode) {
        _uiState.update {
            it.copy(
                mode = mode,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun onFullNameChange(name: String) {
        _uiState.update { it.copy(fullName = name, errorMessage = null) }
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun onConfirmPasswordChange(confirm: String) {
        _uiState.update { it.copy(confirmPassword = confirm, errorMessage = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun submit() {
        val state = _uiState.value
        val email = state.email.trim()
        val password = state.password

        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please fill in all required fields.") }
            return
        }

        if (state.mode == AuthMode.SIGN_UP) {
            val name = state.fullName.trim()
            if (name.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Please enter your full name.") }
                return
            }
            if (password != state.confirmPassword) {
                _uiState.update { it.copy(errorMessage = "Passwords do not match.") }
                return
            }
            if (password.length < 6) {
                _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters.") }
                return
            }

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            viewModelScope.launch {
                val result = authRepository.register(name, email, password)
                _uiState.update { it.copy(isLoading = false) }
                result.onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message ?: "Registration failed.") }
                }
            }
        } else {
            // Sign in
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            viewModelScope.launch {
                val result = authRepository.login(email, password)
                _uiState.update { it.copy(isLoading = false) }
                result.onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message ?: "Login failed.") }
                }
            }
        }
    }

    fun continueAsGuest() {
        authRepository.continueAsGuest()
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(authRepository) as T
        }
    }
}

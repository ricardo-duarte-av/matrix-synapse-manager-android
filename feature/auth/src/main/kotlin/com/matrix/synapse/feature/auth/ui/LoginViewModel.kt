package com.matrix.synapse.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matrix.synapse.feature.auth.domain.LoginUseCase
import com.matrix.synapse.feature.auth.domain.LoginResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LoginState {
    data object Idle : LoginState
    data object Loading : LoginState
    data class Success(val result: LoginResult) : LoginState
    data class Error(val message: String) : LoginState
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<LoginState>(LoginState.Idle)
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun login(serverUrl: String, serverId: String, username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _state.value = LoginState.Error("Username and password are required")
            return
        }
        viewModelScope.launch {
            _state.value = LoginState.Loading
            val result = loginUseCase.login(
                serverUrl = serverUrl,
                serverId = serverId,
                username = username,
                password = password,
            )
            _state.value = result.fold(
                onSuccess = { LoginState.Success(it) },
                onFailure = { LoginState.Error(it.message ?: "Login failed") },
            )
        }
    }

    fun resetState() {
        _state.value = LoginState.Idle
    }
}

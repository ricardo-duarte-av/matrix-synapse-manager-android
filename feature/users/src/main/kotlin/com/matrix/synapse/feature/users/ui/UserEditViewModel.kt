package com.matrix.synapse.feature.users.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matrix.synapse.feature.users.domain.UpsertUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserEditState(
    val isSaving: Boolean = false,
    val error: String? = null,
    val savedUserId: String? = null,
)

@HiltViewModel
class UserEditViewModel @Inject constructor(
    private val upsertUserUseCase: UpsertUserUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(UserEditState())
    val state: StateFlow<UserEditState> = _state.asStateFlow()

    fun createUser(
        serverUrl: String,
        userId: String,
        password: String,
        displayName: String?,
        admin: Boolean,
    ) {
        _state.value = UserEditState(isSaving = true)
        viewModelScope.launch {
            upsertUserUseCase.createUser(serverUrl, userId, password, displayName, admin)
                .onSuccess { _state.value = UserEditState(savedUserId = it.userId) }
                .onFailure { _state.value = UserEditState(error = it.message) }
        }
    }

    fun updateUser(
        serverUrl: String,
        userId: String,
        displayName: String?,
        admin: Boolean?,
    ) {
        _state.value = UserEditState(isSaving = true)
        viewModelScope.launch {
            upsertUserUseCase.updateUser(serverUrl, userId, displayName, admin)
                .onSuccess { _state.value = UserEditState(savedUserId = it.userId) }
                .onFailure { _state.value = UserEditState(error = it.message) }
        }
    }
}

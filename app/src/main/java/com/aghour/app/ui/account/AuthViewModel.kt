package com.aghour.app.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aghour.app.data.remote.ApiResult
import com.aghour.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthFormState(
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val password: String = "",
    val passwordConfirmation: String = "",
    val submitting: Boolean = false,
    val error: String? = null,
    val done: Boolean = false,
)

/** Shared by the login and register screens. */
class AuthViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(AuthFormState())
    val state: StateFlow<AuthFormState> = _state.asStateFlow()

    fun update(transform: AuthFormState.() -> AuthFormState) = _state.update { it.transform().copy(error = null) }

    fun login() = submit { auth.login(it.phone, it.password) }

    fun register() = submit { auth.register(it.name, it.phone, it.email, it.password, it.passwordConfirmation) }

    private fun submit(call: suspend (AuthFormState) -> ApiResult<*>) {
        if (_state.value.submitting) return
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            when (val result = call(_state.value)) {
                is ApiResult.Success -> _state.update { it.copy(submitting = false, done = true) }
                is ApiResult.Failure -> _state.update { it.copy(submitting = false, error = result.message) }
            }
        }
    }
}

package com.aghourservices.ui.account

import com.aghourservices.data.analytics.AnalyticsEvent
import com.aghourservices.data.analytics.AnalyticsEvent.AuthFlow
import com.aghourservices.data.analytics.AnalyticsService
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aghourservices.R
import com.aghourservices.data.remote.ApiResult
import com.aghourservices.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthFormState(
    val firstName: String = "",
    val lastName: String = "",
    val phone: String = "",
    val email: String = "",
    val password: String = "",
    val passwordConfirmation: String = "",
    val submitting: Boolean = false,
    /** Message from the server (already Arabic). */
    val error: String? = null,
    /** Field errors found on the phone before calling the server. */
    @StringRes val nameError: Int? = null,
    @StringRes val phoneError: Int? = null,
    @StringRes val passwordError: Int? = null,
    @StringRes val confirmationError: Int? = null,
    val done: Boolean = false,
)

private val EgyptianMobile = Regex("^01[0125]\\d{8}$")

/** Shared by the login and register screens. */
class AuthViewModel(private val auth: AuthRepository, private val analytics: AnalyticsService) : ViewModel() {
    private val _state = MutableStateFlow(AuthFormState())
    val state: StateFlow<AuthFormState> = _state.asStateFlow()

    /** Any edit clears the errors shown for the form. */
    fun update(transform: AuthFormState.() -> AuthFormState) = _state.update {
        it.transform().copy(error = null, nameError = null, phoneError = null, passwordError = null, confirmationError = null)
    }

    fun login() {
        val s = _state.value
        val phoneError = if (!EgyptianMobile.matches(s.phone.digitsOnly())) R.string.error_phone_invalid else null
        val passwordError = if (s.password.isEmpty()) R.string.error_password_short else null
        if (phoneError != null || passwordError != null) {
            _state.update { it.copy(phoneError = phoneError, passwordError = passwordError) }
            return
        }
        submit(AuthFlow.Login) { auth.login(it.phone.digitsOnly(), it.password) }
    }

    fun register() {
        val s = _state.value
        val nameError = if (s.firstName.isBlank()) R.string.error_name_required else null
        val phoneError = if (!EgyptianMobile.matches(s.phone.digitsOnly())) R.string.error_phone_invalid else null
        val passwordError = if (s.password.length < 8) R.string.error_password_short else null
        val confirmationError = if (s.password != s.passwordConfirmation) R.string.error_password_mismatch else null
        if (listOf(nameError, phoneError, passwordError, confirmationError).any { it != null }) {
            _state.update {
                it.copy(nameError = nameError, phoneError = phoneError, passwordError = passwordError, confirmationError = confirmationError)
            }
            return
        }
        val fullName = "${s.firstName.trim()} ${s.lastName.trim()}".trim()
        submit(AuthFlow.SignUp) { auth.register(fullName, it.phone.digitsOnly(), it.email, it.password, it.passwordConfirmation) }
    }

    private fun submit(flow: AuthFlow, call: suspend (AuthFormState) -> ApiResult<*>) {
        if (_state.value.submitting) return
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            when (val result = call(_state.value)) {
                is ApiResult.Success -> {
                    analytics.log(if (flow == AuthFlow.Login) AnalyticsEvent.Login else AnalyticsEvent.SignUp)
                    _state.update { it.copy(submitting = false, done = true) }
                }
                is ApiResult.Failure -> {
                    analytics.log(AnalyticsEvent.AuthFailed(flow, result.httpStatus))
                    _state.update { it.copy(submitting = false, error = result.message) }
                }
            }
        }
    }

    private fun String.digitsOnly() = filter { it.isDigit() }
}

package com.aghourservices.ui.owner

import com.aghourservices.data.analytics.AnalyticsEvent
import com.aghourservices.data.analytics.AnalyticsService
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aghourservices.R
import com.aghourservices.data.remote.ApiResult
import com.aghourservices.data.repository.AuthRepository
import com.aghourservices.ui.components.ErrorBanner
import com.aghourservices.ui.components.FormField
import com.aghourservices.ui.components.GoldButton
import com.aghourservices.ui.components.ScreenTitle
import com.aghourservices.ui.components.appContainer
import com.aghourservices.ui.theme.Aghour
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChangePasswordState(
    val password: String = "",
    val confirmation: String = "",
    val passwordError: Int? = null,
    val confirmationError: Int? = null,
    val submitting: Boolean = false,
    val error: String? = null,
    val done: Boolean = false,
)

class ChangePasswordViewModel(private val auth: AuthRepository, private val analytics: AnalyticsService) : ViewModel() {
    private val _state = MutableStateFlow(ChangePasswordState())
    val state: StateFlow<ChangePasswordState> = _state.asStateFlow()

    fun update(transform: ChangePasswordState.() -> ChangePasswordState) =
        _state.update { it.transform().copy(error = null, passwordError = null, confirmationError = null) }

    fun submit() {
        val s = _state.value
        val passwordError = if (s.password.length < 8) R.string.error_password_short else null
        val confirmationError = if (s.password != s.confirmation) R.string.error_password_mismatch else null
        if (passwordError != null || confirmationError != null) {
            _state.update { it.copy(passwordError = passwordError, confirmationError = confirmationError) }
            return
        }
        _state.update { it.copy(submitting = true) }
        viewModelScope.launch {
            when (val r = auth.changePassword(s.password, s.confirmation)) {
                is ApiResult.Success -> {
                    analytics.log(AnalyticsEvent.PasswordChanged)
                    _state.update { it.copy(submitting = false, done = true) }
                }
                is ApiResult.Failure -> _state.update { it.copy(submitting = false, error = r.message) }
            }
        }
    }
}

@Composable
fun ChangePasswordScreen(onBack: () -> Unit) {
    val container = appContainer()
    val vm: ChangePasswordViewModel = viewModel { ChangePasswordViewModel(container.auth, container.analytics) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val changed = stringResource(R.string.password_changed)

    LaunchedEffect(state.done) {
        if (state.done) {
            Toast.makeText(context, changed, Toast.LENGTH_SHORT).show()
            onBack()
        }
    }

    Column(Modifier.fillMaxSize().background(Aghour.colors.page).imePadding()) {
        ScreenTitle(stringResource(R.string.change_password), onBack = onBack)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FormField(
                label = stringResource(R.string.new_password),
                value = state.password,
                onChange = { v -> vm.update { copy(password = v) } },
                error = state.passwordError,
                isPassword = true,
            )
            FormField(
                label = stringResource(R.string.password_confirmation),
                value = state.confirmation,
                onChange = { v -> vm.update { copy(confirmation = v) } },
                error = state.confirmationError,
                imeAction = ImeAction.Done,
                onDone = vm::submit,
                isPassword = true,
            )
            ErrorBanner(state.error)
            GoldButton(stringResource(R.string.save), vm::submit, loading = state.submitting)
        }
    }
}

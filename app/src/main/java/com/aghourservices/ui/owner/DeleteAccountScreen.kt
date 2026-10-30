package com.aghourservices.ui.owner

import com.aghourservices.data.analytics.AnalyticsEvent
import com.aghourservices.data.analytics.AnalyticsService
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.aghourservices.ui.components.MutedText
import com.aghourservices.ui.components.ScreenTitle
import com.aghourservices.ui.components.appContainer
import com.aghourservices.ui.theme.Aghour
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DeleteAccountState(
    val password: String = "",
    val passwordError: Int? = null,
    val confirming: Boolean = false,
    val submitting: Boolean = false,
    val error: String? = null,
    val done: Boolean = false,
)

class DeleteAccountViewModel(private val auth: AuthRepository, private val analytics: AnalyticsService) : ViewModel() {
    private val _state = MutableStateFlow(DeleteAccountState())
    val state: StateFlow<DeleteAccountState> = _state.asStateFlow()

    fun setPassword(value: String) = _state.update { it.copy(password = value, passwordError = null, error = null) }

    /** Validates locally, then asks for a final confirmation before anything is sent. */
    fun requestDelete() {
        if (_state.value.password.isBlank()) {
            _state.update { it.copy(passwordError = R.string.error_password_required) }
            return
        }
        _state.update { it.copy(confirming = true) }
    }

    fun dismissConfirm() = _state.update { it.copy(confirming = false) }

    fun delete() {
        if (_state.value.submitting) return
        _state.update { it.copy(confirming = false, submitting = true) }
        viewModelScope.launch {
            when (val r = auth.deleteAccount(_state.value.password)) {
                is ApiResult.Success -> {
                    analytics.log(AnalyticsEvent.AccountDeleted)
                    _state.update { it.copy(submitting = false, done = true) }
                }
                is ApiResult.Failure -> _state.update { it.copy(submitting = false, error = r.message) }
            }
        }
    }
}

@Composable
fun DeleteAccountScreen(onBack: () -> Unit) {
    val container = appContainer()
    val vm: DeleteAccountViewModel = viewModel { DeleteAccountViewModel(container.auth, container.analytics) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val deleted = stringResource(R.string.account_deleted)
    val c = Aghour.colors

    LaunchedEffect(state.done) {
        if (state.done) {
            Toast.makeText(context, deleted, Toast.LENGTH_SHORT).show()
            onBack()
        }
    }

    Column(Modifier.fillMaxSize().background(c.page).imePadding()) {
        ScreenTitle(stringResource(R.string.delete_account), onBack = onBack)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MutedText(stringResource(R.string.delete_account_body))
            FormField(
                label = stringResource(R.string.delete_account_password_hint),
                value = state.password,
                onChange = vm::setPassword,
                error = state.passwordError,
                imeAction = ImeAction.Done,
                onDone = vm::requestDelete,
                isPassword = true,
            )
            ErrorBanner(state.error)
            Button(
                onClick = vm::requestDelete,
                enabled = !state.submitting,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = c.bad.copy(alpha = 0.10f), contentColor = c.bad),
            ) {
                if (state.submitting) {
                    CircularProgressIndicator(Modifier.padding(4.dp), color = c.bad, strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.delete_account), fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (state.confirming) {
        AlertDialog(
            onDismissRequest = vm::dismissConfirm,
            title = { Text(stringResource(R.string.delete_account_confirm_title)) },
            text = { Text(stringResource(R.string.delete_account_confirm_body)) },
            confirmButton = {
                TextButton(onClick = vm::delete) { Text(stringResource(R.string.delete_account_confirm), color = c.bad) }
            },
            dismissButton = { TextButton(onClick = vm::dismissConfirm) { Text(stringResource(R.string.cancel), color = c.ink) } },
        )
    }
}

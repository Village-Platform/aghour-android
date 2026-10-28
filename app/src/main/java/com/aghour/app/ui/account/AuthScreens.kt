package com.aghour.app.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aghour.app.R
import com.aghour.app.ui.components.BrandLogo
import com.aghour.app.ui.components.ErrorBanner
import com.aghour.app.ui.components.FormField
import com.aghour.app.ui.components.GoldButton
import com.aghour.app.ui.components.StatusBarIcons
import com.aghour.app.ui.components.appContainer
import com.aghour.app.ui.theme.Aghour

@Composable
fun LoginScreen(onBack: () -> Unit, onDone: () -> Unit, onGoToRegister: () -> Unit) {
    val container = appContainer()
    val vm: AuthViewModel = viewModel { AuthViewModel(container.auth) }
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone() }

    AuthLayout(stringResource(R.string.login_title), stringResource(R.string.login_hint), onBack, showLogo = true) {
        FormField(
            label = stringResource(R.string.phone_number),
            value = state.phone,
            onChange = { v -> vm.update { copy(phone = v.filter { it.isDigit() }.take(11)) } },
            placeholder = "01X XXXX XXXX",
            error = state.phoneError,
            keyboardType = KeyboardType.Phone,
            ltr = true,
        )
        FormField(
            label = stringResource(R.string.password),
            value = state.password,
            onChange = { v -> vm.update { copy(password = v) } },
            placeholder = "••••••••",
            error = state.passwordError,
            imeAction = ImeAction.Done,
            onDone = vm::login,
            isPassword = true,
        )
        ErrorBanner(state.error)
        GoldButton(stringResource(R.string.login), vm::login, loading = state.submitting)
        SwitchLink(stringResource(R.string.no_account_q), stringResource(R.string.create_account_link), onGoToRegister)
    }
}

@Composable
fun RegisterScreen(onBack: () -> Unit, onDone: () -> Unit, onGoToLogin: () -> Unit) {
    val container = appContainer()
    val vm: AuthViewModel = viewModel { AuthViewModel(container.auth) }
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone() }

    AuthLayout(stringResource(R.string.register_title), stringResource(R.string.register_hint), onBack, showLogo = false) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FormField(
                label = stringResource(R.string.first_name),
                value = state.firstName,
                onChange = { v -> vm.update { copy(firstName = v) } },
                placeholder = "أحمد",
                error = state.nameError,
                modifier = Modifier.weight(1f),
            )
            FormField(
                label = stringResource(R.string.last_name),
                value = state.lastName,
                onChange = { v -> vm.update { copy(lastName = v) } },
                placeholder = "محمد",
                modifier = Modifier.weight(1f),
            )
        }
        FormField(
            label = stringResource(R.string.email_optional),
            value = state.email,
            onChange = { v -> vm.update { copy(email = v) } },
            placeholder = "you@email.com",
            keyboardType = KeyboardType.Email,
            ltr = true,
        )
        FormField(
            label = stringResource(R.string.phone_number),
            value = state.phone,
            onChange = { v -> vm.update { copy(phone = v.filter { it.isDigit() }.take(11)) } },
            placeholder = "01X XXXX XXXX",
            error = state.phoneError,
            keyboardType = KeyboardType.Phone,
            ltr = true,
        )
        FormField(
            label = stringResource(R.string.password),
            value = state.password,
            onChange = { v -> vm.update { copy(password = v) } },
            placeholder = stringResource(R.string.error_password_short),
            error = state.passwordError,
            isPassword = true,
        )
        FormField(
            label = stringResource(R.string.password_confirmation),
            value = state.passwordConfirmation,
            onChange = { v -> vm.update { copy(passwordConfirmation = v) } },
            error = state.confirmationError,
            imeAction = ImeAction.Done,
            onDone = vm::register,
            isPassword = true,
        )
        ErrorBanner(state.error)
        GoldButton(stringResource(R.string.create_account), vm::register, loading = state.submitting)
        SwitchLink(stringResource(R.string.have_account_q), stringResource(R.string.login), onGoToLogin)
    }
}

/** Plain page: back button, optional logo, title and the form (login/register designs). */
@Composable
private fun AuthLayout(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    showLogo: Boolean,
    form: @Composable () -> Unit,
) {
    val c = Aghour.colors
    StatusBarIcons()
    Column(
        Modifier
            .fillMaxSize()
            .background(c.page)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(bottom = 24.dp),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.padding(start = 4.dp, top = 4.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = c.ink)
        }
        Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (showLogo) {
                BrandLogo(Modifier.height(112.dp).align(Alignment.CenterHorizontally).padding(vertical = 4.dp))
            }
            Column {
                Text(title, color = c.ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(subtitle, color = c.muted, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
            form()
        }
    }
}

@Composable
private fun SwitchLink(question: String, action: String, onClick: () -> Unit) {
    val c = Aghour.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Text(question, color = c.muted, style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = onClick) { Text(action, color = c.ink, fontWeight = FontWeight.Bold) }
    }
}

package com.aghour.app.ui.account

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aghour.app.R
import com.aghour.app.ui.components.AppTopBar
import com.aghour.app.ui.components.appContainer

@Composable
fun LoginScreen(onBack: () -> Unit, onDone: () -> Unit, onGoToRegister: () -> Unit) {
    val container = appContainer()
    val vm: AuthViewModel = viewModel { AuthViewModel(container.auth) }
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone() }

    AuthScaffold(stringResource(R.string.login), onBack) {
        PhoneField(state.phone) { v -> vm.update { copy(phone = v) } }
        PasswordField(stringResource(R.string.password), state.password, ImeAction.Done) { v -> vm.update { copy(password = v) } }
        ErrorText(state.error)
        SubmitButton(stringResource(R.string.login), state.submitting, vm::login)
        TextButton(onClick = onGoToRegister) { Text(stringResource(R.string.no_account)) }
    }
}

@Composable
fun RegisterScreen(onBack: () -> Unit, onDone: () -> Unit, onGoToLogin: () -> Unit) {
    val container = appContainer()
    val vm: AuthViewModel = viewModel { AuthViewModel(container.auth) }
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone() }

    AuthScaffold(stringResource(R.string.register), onBack) {
        OutlinedTextField(
            value = state.name,
            onValueChange = { v -> vm.update { copy(name = v) } },
            label = { Text(stringResource(R.string.name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        PhoneField(state.phone) { v -> vm.update { copy(phone = v) } }
        OutlinedTextField(
            value = state.email,
            onValueChange = { v -> vm.update { copy(email = v) } },
            label = { Text(stringResource(R.string.email_optional)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        )
        PasswordField(stringResource(R.string.password), state.password, ImeAction.Next) { v -> vm.update { copy(password = v) } }
        PasswordField(stringResource(R.string.password_confirmation), state.passwordConfirmation, ImeAction.Done) { v ->
            vm.update { copy(passwordConfirmation = v) }
        }
        ErrorText(state.error)
        SubmitButton(stringResource(R.string.register), state.submitting, vm::register)
        TextButton(onClick = onGoToLogin) { Text(stringResource(R.string.have_account)) }
    }
}

@Composable
private fun AuthScaffold(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Scaffold(topBar = { AppTopBar(title, onBack) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(painterResource(R.drawable.brand_logo), contentDescription = null, modifier = Modifier.size(120.dp))
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
private fun PhoneField(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(R.string.phone_number)) },
        placeholder = { Text("01xxxxxxxxx") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
    )
}

@Composable
private fun PasswordField(label: String, value: String, imeAction: ImeAction, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
    )
}

@Composable
private fun ErrorText(error: String?) {
    if (error != null) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun SubmitButton(label: String, submitting: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = !submitting, modifier = Modifier.fillMaxWidth().height(52.dp)) {
        if (submitting) CircularProgressIndicator(Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
        else Text(label)
    }
}

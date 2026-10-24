package com.aghour.app.ui.account

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aghour.app.R
import com.aghour.app.ui.components.BrandHeader
import com.aghour.app.ui.components.appContainer
import com.aghour.app.ui.theme.Cream
import com.aghour.app.ui.theme.Gold400
import com.aghour.app.ui.theme.Gold500

@Composable
fun LoginScreen(onBack: () -> Unit, onDone: () -> Unit, onGoToRegister: () -> Unit) {
    val container = appContainer()
    val vm: AuthViewModel = viewModel { AuthViewModel(container.auth) }
    val state by vm.state.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current
    LaunchedEffect(state.done) { if (state.done) onDone() }

    AuthLayout(stringResource(R.string.welcome_back), stringResource(R.string.login_subtitle), onBack) {
        PhoneField(state.phone, state.phoneError) { v -> vm.update { copy(phone = v) } }
        PasswordField(
            label = stringResource(R.string.password),
            value = state.password,
            error = state.passwordError,
            imeAction = ImeAction.Done,
            onDone = { focus.clearFocus(); vm.login() },
        ) { v -> vm.update { copy(password = v) } }
        ServerError(state.error)
        SubmitButton(stringResource(R.string.login), state.submitting) { focus.clearFocus(); vm.login() }
        TextButton(onClick = onGoToRegister, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.no_account)) }
    }
}

@Composable
fun RegisterScreen(onBack: () -> Unit, onDone: () -> Unit, onGoToLogin: () -> Unit) {
    val container = appContainer()
    val vm: AuthViewModel = viewModel { AuthViewModel(container.auth) }
    val state by vm.state.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current
    LaunchedEffect(state.done) { if (state.done) onDone() }

    AuthLayout(stringResource(R.string.register), stringResource(R.string.register_subtitle), onBack) {
        BrandField(
            value = state.name,
            label = stringResource(R.string.name),
            icon = Icons.Filled.Person,
            error = state.nameError,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        ) { v -> vm.update { copy(name = v) } }
        PhoneField(state.phone, state.phoneError) { v -> vm.update { copy(phone = v) } }
        BrandField(
            value = state.email,
            label = stringResource(R.string.email_optional),
            icon = Icons.Filled.Email,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            ltr = true,
        ) { v -> vm.update { copy(email = v) } }
        PasswordField(stringResource(R.string.password), state.password, state.passwordError, ImeAction.Next) { v ->
            vm.update { copy(password = v) }
        }
        PasswordField(
            label = stringResource(R.string.password_confirmation),
            value = state.passwordConfirmation,
            error = state.confirmationError,
            imeAction = ImeAction.Done,
            onDone = { focus.clearFocus(); vm.register() },
        ) { v -> vm.update { copy(passwordConfirmation = v) } }
        ServerError(state.error)
        SubmitButton(stringResource(R.string.register), state.submitting) { focus.clearFocus(); vm.register() }
        TextButton(onClick = onGoToLogin, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.have_account)) }
    }
}

/** Green header with the logo, then a white card holding the form (overlapping the header). */
@Composable
private fun AuthLayout(title: String, subtitle: String, onBack: () -> Unit, form: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState()),
    ) {
        BrandHeader {
            Column(
                Modifier.fillMaxWidth().statusBarsPadding().padding(bottom = 64.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = Cream)
                    }
                }
                Image(painterResource(R.drawable.brand_mark_light), contentDescription = null, modifier = Modifier.size(84.dp))
                Spacer(Modifier.height(10.dp))
                Text(title, color = Cream, style = MaterialTheme.typography.headlineSmall)
                Text(subtitle, color = Gold400, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .offset(y = (-44).dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { form() }
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun BrandField(
    value: String,
    label: String,
    icon: ImageVector,
    error: Int? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
    ltr: Boolean = false,
    onChange: (String) -> Unit,
) {
    val focus = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = Gold500) },
        trailingIcon = trailing,
        isError = error != null,
        supportingText = if (error != null) {
            { Text(stringResource(error)) }
        } else {
            null
        },
        singleLine = true,
        visualTransformation = visualTransformation,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = keyboardOptions,
        keyboardActions = if (keyboardActions == KeyboardActions.Default) {
            KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Next) })
        } else {
            keyboardActions
        },
        // Phone numbers and emails are typed left-to-right.
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            textDirection = if (ltr) TextDirection.Ltr else TextDirection.Content,
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

@Composable
private fun PhoneField(value: String, error: Int?, onChange: (String) -> Unit) {
    BrandField(
        value = value,
        label = stringResource(R.string.phone_number),
        icon = Icons.Filled.Phone,
        error = error,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
        ltr = true,
        onChange = { onChange(it.filter { c -> c.isDigit() }.take(11)) },
    )
}

@Composable
private fun PasswordField(
    label: String,
    value: String,
    error: Int?,
    imeAction: ImeAction,
    onDone: (() -> Unit)? = null,
    onChange: (String) -> Unit,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    BrandField(
        value = value,
        label = label,
        icon = Icons.Filled.Lock,
        error = error,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        keyboardActions = if (onDone != null) KeyboardActions(onDone = { onDone() }) else KeyboardActions.Default,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailing = {
            TextButton(onClick = { visible = !visible }) {
                Text(stringResource(if (visible) R.string.hide_password else R.string.show_password))
            }
        },
        onChange = onChange,
    )
}

@Composable
private fun ServerError(error: String?) {
    if (error == null) return
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(8.dp))
        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SubmitButton(label: String, submitting: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !submitting,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(14.dp),
    ) {
        if (submitting) {
            CircularProgressIndicator(Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
        } else {
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    }
}


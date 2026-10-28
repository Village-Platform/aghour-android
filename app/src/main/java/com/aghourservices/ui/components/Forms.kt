package com.aghourservices.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.aghourservices.R
import com.aghourservices.ui.theme.Aghour

/** Label above a soft grey rounded field with a thin border (as in the designs). */
@Composable
fun FormField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    @StringRes error: Int? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onDone: (() -> Unit)? = null,
    ltr: Boolean = false,
    isPassword: Boolean = false,
) {
    val focus = LocalFocusManager.current
    val c = Aghour.colors
    var visible by rememberSaveable { mutableStateOf(false) }
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = c.muted)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = if (placeholder != null) {
                { Text(placeholder, color = c.muted.copy(alpha = 0.6f)) }
            } else {
                null
            },
            singleLine = true,
            isError = error != null,
            supportingText = if (error != null) {
                { Text(stringResource(error)) }
            } else {
                null
            },
            shape = RoundedCornerShape(12.dp),
            visualTransformation = if (isPassword && !visible) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = if (isPassword) {
                {
                    TextButton(onClick = { visible = !visible }) {
                        Text(stringResource(if (visible) R.string.hide_password else R.string.show_password), color = c.goldInk)
                    }
                }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
                imeAction = imeAction,
            ),
            keyboardActions = KeyboardActions(
                onNext = { focus.moveFocus(FocusDirection.Next) },
                onDone = { focus.clearFocus(); onDone?.invoke() },
            ),
            // Phone numbers and emails are typed left-to-right.
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                textDirection = if (ltr) TextDirection.Ltr else TextDirection.Content,
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = c.surface2,
                unfocusedContainerColor = c.surface2,
                errorContainerColor = c.surface2,
                focusedTextColor = c.ink,
                unfocusedTextColor = c.ink,
                focusedBorderColor = c.gold,
                unfocusedBorderColor = c.line,
                errorBorderColor = c.bad,
                errorSupportingTextColor = c.bad,
                cursorColor = c.gold,
            ),
        )
    }
}

@Composable
fun ErrorBanner(message: String?) {
    if (message == null) return
    val c = Aghour.colors
    Row(
        Modifier.fillMaxWidth().background(c.bad.copy(alpha = 0.10f), RoundedCornerShape(12.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = c.bad, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(8.dp))
        Text(message, color = c.bad, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun MutedText(text: String, modifier: Modifier = Modifier) =
    Text(text, modifier = modifier, color = Aghour.colors.muted, style = MaterialTheme.typography.bodySmall)

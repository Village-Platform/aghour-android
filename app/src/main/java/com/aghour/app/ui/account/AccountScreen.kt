package com.aghour.app.ui.account

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aghour.app.BuildConfig
import com.aghour.app.R
import com.aghour.app.data.remote.UserDto
import com.aghour.app.ui.components.BrandHeader
import com.aghour.app.ui.components.LetterAvatar
import com.aghour.app.ui.components.Pill
import com.aghour.app.ui.components.appContainer
import com.aghour.app.ui.components.brandCardColors
import com.aghour.app.ui.components.brandCardElevation
import com.aghour.app.ui.components.roleLabel
import com.aghour.app.ui.theme.Cream
import com.aghour.app.ui.theme.Gold100
import com.aghour.app.ui.theme.Gold400
import com.aghour.app.ui.theme.Gold500
import com.aghour.app.ui.theme.Green900
import com.aghour.app.ui.theme.Green950
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(onLogin: () -> Unit, onRegister: () -> Unit) {
    val container = appContainer()
    val user by container.auth.currentUser.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var confirmLogout by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }

    // Refresh the profile when signed in (the role changes after a subscription is approved).
    LaunchedEffect(user?.id) { if (user != null) container.auth.refreshMe() }

    val current = user
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        if (current == null) GuestHeader() else ProfileHeader(current)

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (current == null) {
                GuestActions(onLogin, onRegister)
            } else {
                Card(shape = RoundedCornerShape(20.dp), colors = brandCardColors(), elevation = brandCardElevation()) {
                    MenuRow(Icons.Filled.Phone, stringResource(R.string.phone_number), current.phone)
                    current.email?.let {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        MenuRow(Icons.Filled.Email, stringResource(R.string.email_optional), it)
                    }
                }
            }

            Card(shape = RoundedCornerShape(20.dp), colors = brandCardColors(), elevation = brandCardElevation()) {
                MenuRow(Icons.Filled.Info, stringResource(R.string.about_app), onClick = { showAbout = true })
                if (current != null) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    MenuRow(
                        Icons.AutoMirrored.Filled.ExitToApp,
                        stringResource(R.string.logout),
                        tint = MaterialTheme.colorScheme.error,
                        onClick = { confirmLogout = true },
                    )
                }
            }
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text(stringResource(R.string.logout_confirm_title)) },
            text = { Text(stringResource(R.string.logout_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    scope.launch { container.auth.logout() }
                }) { Text(stringResource(R.string.logout), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            icon = { Image(painterResource(R.drawable.brand_logo), contentDescription = null, modifier = Modifier.size(96.dp)) },
            title = { Text(stringResource(R.string.app_name)) },
            text = { Text(stringResource(R.string.about_app_body, BuildConfig.VERSION_NAME), textAlign = TextAlign.Center) },
            confirmButton = { TextButton(onClick = { showAbout = false }) { Text(stringResource(R.string.ok)) } },
        )
    }
}

@Composable
private fun ProfileHeader(user: UserDto) {
    BrandHeader {
        Column(
            Modifier.fillMaxWidth().statusBarsPadding().padding(top = 24.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LetterAvatar(user.name, size = 84.dp, background = Gold500, content = Green950, bordered = true)
            Spacer(Modifier.height(12.dp))
            Text(user.name, color = Cream, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(6.dp))
            Pill(roleLabel(user.role), Gold100, Green900)
        }
    }
}

@Composable
private fun GuestHeader() {
    BrandHeader {
        Column(
            Modifier.fillMaxWidth().statusBarsPadding().padding(top = 24.dp, bottom = 28.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(painterResource(R.drawable.brand_mark_light), contentDescription = null, modifier = Modifier.size(90.dp))
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.account_guest_title), color = Cream, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Text(stringResource(R.string.account_guest_body), color = Gold400, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun GuestActions(onLogin: () -> Unit, onRegister: () -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), colors = brandCardColors(), elevation = brandCardElevation()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Benefit(stringResource(R.string.benefit_firms))
            Benefit(stringResource(R.string.benefit_notifications))
            Benefit(stringResource(R.string.benefit_free))
            Spacer(Modifier.height(6.dp))
            Button(onClick = onLogin, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
                Text(stringResource(R.string.login), style = MaterialTheme.typography.titleMedium)
            }
            OutlinedButton(onClick = onRegister, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
                Text(stringResource(R.string.register), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun Benefit(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Gold500, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    title: String,
    value: String? = null,
    tint: Color = Green900,
    onClick: (() -> Unit)? = null,
) {
    ListItem(
        headlineContent = { Text(title, color = if (tint == Green900) MaterialTheme.colorScheme.onSurface else tint) },
        // \u2066…\u2069 keeps phone numbers left-to-right inside Arabic text.
        supportingContent = if (value != null) {
            { Text("\u2066$value\u2069", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            null
        },
        leadingContent = { Icon(icon, contentDescription = null, tint = tint) },
        trailingContent = if (onClick != null) {
            { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline) }
        } else {
            null
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
    )
}

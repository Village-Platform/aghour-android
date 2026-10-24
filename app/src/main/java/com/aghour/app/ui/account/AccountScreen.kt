package com.aghour.app.ui.account

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aghour.app.R
import com.aghour.app.data.remote.UserDto
import com.aghour.app.ui.components.AppTopBar
import com.aghour.app.ui.components.Pill
import com.aghour.app.ui.components.appContainer
import com.aghour.app.ui.components.roleLabel
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(onLogin: () -> Unit, onRegister: () -> Unit) {
    val container = appContainer()
    val user by container.auth.currentUser.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // Refresh the profile when signed in (the role changes after a subscription is approved).
    LaunchedEffect(user?.id) { if (user != null) container.auth.refreshMe() }

    Scaffold(topBar = { AppTopBar(stringResource(R.string.tab_account)) }) { padding ->
        val current = user
        Column(
            Modifier.padding(padding).fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (current == null) {
                GuestContent(onLogin, onRegister)
            } else {
                ProfileCard(current)
                OutlinedButton(onClick = { scope.launch { container.auth.logout() } }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                    Text("  " + stringResource(R.string.logout))
                }
            }
        }
    }
}

@Composable
private fun GuestContent(onLogin: () -> Unit, onRegister: () -> Unit) {
    Spacer(Modifier.height(24.dp))
    Image(painterResource(R.drawable.brand_logo), contentDescription = null, modifier = Modifier.size(150.dp))
    Text(stringResource(R.string.account_guest_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    Text(stringResource(R.string.account_guest_body), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
    Button(onClick = onLogin, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(stringResource(R.string.login)) }
    OutlinedButton(onClick = onRegister, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(stringResource(R.string.register)) }
}

@Composable
private fun ProfileCard(user: UserDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center,
            ) {
                Text(user.name.take(1), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondary)
            }
            Spacer(Modifier.height(12.dp))
            Text(user.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(user.phone, color = MaterialTheme.colorScheme.onSurfaceVariant)
            user.email?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Spacer(Modifier.height(10.dp))
            Pill(roleLabel(user.role), MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary)
        }
    }
}

package com.aghour.app.ui.account

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aghour.app.BuildConfig
import com.aghour.app.R
import com.aghour.app.data.remote.ApiResult
import com.aghour.app.data.remote.FirmDto
import com.aghour.app.data.remote.SubscriptionDto
import com.aghour.app.data.remote.UserDto
import com.aghour.app.data.repository.AuthRepository
import com.aghour.app.data.repository.OwnerRepository
import com.aghour.app.ui.components.Badge
import com.aghour.app.ui.components.BadgeTone
import com.aghour.app.ui.components.GoldButton
import com.aghour.app.ui.components.LetterAvatar
import com.aghour.app.ui.components.MutedText
import com.aghour.app.ui.components.SandCard
import com.aghour.app.ui.components.SheetLayout
import com.aghour.app.ui.components.appContainer
import com.aghour.app.ui.components.roleLabel
import com.aghour.app.ui.theme.Cream
import com.aghour.app.ui.theme.Danger
import com.aghour.app.ui.theme.DangerSoft
import com.aghour.app.ui.theme.Forest
import com.aghour.app.ui.theme.Gold
import com.aghour.app.ui.theme.Sand
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val firms: List<FirmDto> = emptyList(),
    val current: SubscriptionDto? = null,
    val pending: SubscriptionDto? = null,
)

class ProfileViewModel(private val auth: AuthRepository, private val owner: OwnerRepository) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    /** Called whenever the profile is shown while signed in (role/subscription may have changed). */
    fun refresh() {
        viewModelScope.launch {
            auth.refreshMe()
            val subs = async { owner.subscriptions() }
            val firms = async { owner.myFirms() }
            val list = (subs.await() as? ApiResult.Success)?.data.orEmpty()
            _state.value = ProfileUiState(
                firms = (firms.await() as? ApiResult.Success)?.data.orEmpty(),
                current = list.firstOrNull { it.state == "current" },
                pending = list.firstOrNull { it.state == "pending" },
            )
        }
    }
}

@Composable
fun AccountScreen(
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    onPlans: () -> Unit,
    onPayments: () -> Unit,
    onNotifications: () -> Unit,
    onChangePassword: () -> Unit,
    onMyFirms: () -> Unit,
    onDeleteAccount: () -> Unit,
) {
    val container = appContainer()
    val user by container.auth.currentUser.collectAsStateWithLifecycle()
    val vm: ProfileViewModel = viewModel { ProfileViewModel(container.auth, container.owner) }
    val state by vm.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var confirmLogout by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(user?.id) { if (user != null) vm.refresh() }

    val current = user
    if (current == null) {
        GuestProfile(onLogin, onRegister, onAbout = { showAbout = true })
    } else {
        SheetLayout(
            sheetPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 24.dp),
            header = { ProfileHeader(current) },
        ) {
            SubscriptionCard(state, onPlans)
            SandCard(padding = 0.dp) {
                if (state.firms.isNotEmpty()) {
                    MenuRow("🏪", stringResource(R.string.my_firms), "${state.firms.size}", onMyFirms)
                    MenuDivider()
                }
                MenuRow("💳", stringResource(R.string.subscription_and_plans), null, onPlans)
                MenuDivider()
                MenuRow("💰", stringResource(R.string.payments_history), null, onPayments)
                MenuDivider()
                MenuRow("🔔", stringResource(R.string.notifications), null, onNotifications)
                MenuDivider()
                MenuRow("🔒", stringResource(R.string.change_password), null, onChangePassword)
                MenuDivider()
                MenuRow("ℹ️", stringResource(R.string.about_app), null) { showAbout = true }
                MenuDivider()
                MenuRow("🗑️", stringResource(R.string.delete_account), null, onDeleteAccount)
            }
            Button(
                onClick = { confirmLogout = true },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DangerSoft, contentColor = Danger),
            ) { Text(stringResource(R.string.logout), fontWeight = FontWeight.Bold) }
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
                }) { Text(stringResource(R.string.logout), color = Danger) }
            },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text(stringResource(R.string.cancel), color = Forest) } },
        )
    }
    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            icon = { Image(painterResource(R.drawable.logo_green), contentDescription = null, modifier = Modifier.size(110.dp)) },
            text = { Text(stringResource(R.string.about_app_body, BuildConfig.VERSION_NAME), textAlign = TextAlign.Center) },
            confirmButton = { TextButton(onClick = { showAbout = false }) { Text(stringResource(R.string.ok), color = Forest) } },
        )
    }
}

@Composable
private fun ProfileHeader(user: UserDto) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LetterAvatar(user.name, size = 60.dp)
        Spacer(Modifier.width(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(user.name, color = Cream, style = MaterialTheme.typography.titleMedium)
            Text("⁦${user.phone}⁩", color = Cream.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
            Badge(roleLabel(user.role), BadgeTone.Gold, Modifier.background(Cream, RoundedCornerShape(50)))
        }
    }
}

/** Current plan with days left and a progress bar, a pending request, or a call to subscribe. */
@Composable
private fun SubscriptionCard(state: ProfileUiState, onPlans: () -> Unit) {
    SandCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                state.firms.firstOrNull()?.name ?: stringResource(R.string.subscription_and_plans),
                style = MaterialTheme.typography.titleSmall,
                color = Forest,
                modifier = Modifier.weight(1f),
            )
            when {
                state.current != null -> Badge("✓ موثّق")
                state.pending != null -> Badge("قيد المراجعة", BadgeTone.Gold)
            }
        }
        Spacer(Modifier.height(10.dp))
        val current = state.current
        if (current != null) {
            val total = current.plan.durationDays.coerceAtLeast(1)
            val left = (current.daysLeft ?: 0).coerceIn(0, total)
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Cream).padding(12.dp)) {
                Row {
                    Text("باقة ${current.plan.name}", style = MaterialTheme.typography.labelMedium, color = Forest, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.days_left, left), style = MaterialTheme.typography.labelMedium, color = Forest)
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { left.toFloat() / total },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                    color = Gold,
                    trackColor = Sand,
                    strokeCap = StrokeCap.Round,
                    drawStopIndicator = {},
                )
            }
        } else if (state.pending == null) {
            MutedText(stringResource(R.string.no_subscription))
            Spacer(Modifier.height(10.dp))
            GoldButton(stringResource(R.string.subscribe_now), onPlans)
        } else {
            MutedText(stringResource(R.string.payment_sent_body))
        }
    }
}

@Composable
private fun MenuRow(emoji: String, title: String, trailing: String?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji)
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = Forest, modifier = Modifier.weight(1f))
        if (trailing != null) {
            Box(Modifier.background(Gold.copy(alpha = 0.15f), CircleShape).padding(horizontal = 8.dp)) {
                Text(trailing, style = MaterialTheme.typography.labelSmall, color = Forest)
            }
            Spacer(Modifier.width(8.dp))
        }
        Text("‹", color = Forest.copy(alpha = 0.3f), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun MenuDivider() = HorizontalDivider(color = Sand.copy(alpha = 0.6f))

@Composable
private fun GuestProfile(onLogin: () -> Unit, onRegister: () -> Unit, onAbout: () -> Unit) {
    SheetLayout(
        sheetPadding = PaddingValues(20.dp),
        header = {
            Column(
                Modifier.fillMaxWidth().statusBarsPadding().padding(top = 16.dp, bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(painterResource(R.drawable.logo_cream), contentDescription = null, modifier = Modifier.height(120.dp))
            }
        },
    ) {
        Text(stringResource(R.string.guest_profile_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Forest)
        MutedText(stringResource(R.string.guest_profile_body))
        listOf(R.string.benefit_firms, R.string.benefit_notifications, R.string.benefit_free).forEach { res ->
            Text("✓ ${stringResource(res)}", style = MaterialTheme.typography.bodyMedium, color = Forest)
        }
        GoldButton(stringResource(R.string.login), onLogin)
        OutlinedButton(
            onClick = onRegister,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
        ) { Text(stringResource(R.string.register), color = Forest, fontWeight = FontWeight.Bold) }
        TextButton(onClick = onAbout, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.about_app), color = Gold) }
    }
}

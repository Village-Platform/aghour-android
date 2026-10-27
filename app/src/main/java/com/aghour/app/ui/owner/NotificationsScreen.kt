package com.aghour.app.ui.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aghour.app.R
import com.aghour.app.data.remote.ApiResult
import com.aghour.app.data.remote.NotificationDto
import com.aghour.app.data.repository.OwnerRepository
import com.aghour.app.ui.components.FilterChipsRow
import com.aghour.app.ui.components.MessageBox
import com.aghour.app.ui.components.ScreenTitle
import com.aghour.app.ui.components.SkeletonRows
import com.aghour.app.ui.components.appContainer
import com.aghour.app.ui.components.notificationSymbol
import com.aghour.app.ui.components.relativeTime
import com.aghour.app.ui.theme.Aghour
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val loading: Boolean = true,
    val items: List<NotificationDto> = emptyList(),
    /** "غير مقروء" filter chip. */
    val unreadOnly: Boolean = false,
    val error: String? = null,
) {
    val unreadCount: Int get() = items.count { !it.read }
    val visible: List<NotificationDto> get() = if (unreadOnly) items.filter { !it.read } else items
}

class NotificationsViewModel(private val owner: OwnerRepository) : ViewModel() {
    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = owner.notifications()) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, items = r.data.first) }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = r.message) }
            }
        }
    }

    fun showUnreadOnly(unreadOnly: Boolean) = _state.update { it.copy(unreadOnly = unreadOnly) }

    /** Optimistic: mark as read on screen right away, then tell the server. */
    fun markRead(id: Long) {
        _state.update { s -> s.copy(items = s.items.map { if (it.id == id) it.copy(read = true) else it }) }
        viewModelScope.launch { owner.markRead(id) }
    }

    fun markAllRead() {
        _state.update { s -> s.copy(items = s.items.map { it.copy(read = true) }) }
        viewModelScope.launch { owner.markAllRead() }
    }
}

/** Where a notification row's gold "←" link leads, guessed from the server's `kind`. */
enum class NotificationTarget(val labelRes: Int) {
    Payments(R.string.notif_open_subscription),
    Plans(R.string.notif_open_plans),
    MyFirms(R.string.notif_open_firm),
    News(R.string.notif_open_news),
}

fun notificationTarget(kind: String): NotificationTarget? = when {
    "expir" in kind || "renew" in kind -> NotificationTarget.Plans
    "payment" in kind || "subscription" in kind -> NotificationTarget.Payments
    "firm" in kind -> NotificationTarget.MyFirms
    "article" in kind || "news" in kind -> NotificationTarget.News
    else -> null
}

@Composable
fun NotificationsScreen(onBack: () -> Unit, onOpen: (NotificationTarget) -> Unit) {
    val container = appContainer()
    val vm: NotificationsViewModel = viewModel { NotificationsViewModel(container.owner) }
    val state by vm.state.collectAsStateWithLifecycle()
    val c = Aghour.colors

    Column(Modifier.fillMaxSize().background(c.page)) {
        ScreenTitle(stringResource(R.string.notifications), onBack = onBack, actions = {
            if (state.unreadCount > 0) {
                TextButton(onClick = vm::markAllRead) { Text(stringResource(R.string.mark_all_read), color = c.goldInk) }
            }
        })
        if (state.items.isNotEmpty()) {
            FilterChipsRow(
                options = listOf(
                    false to stringResource(R.string.all),
                    true to stringResource(R.string.unread_count, state.unreadCount),
                ),
                selected = state.unreadOnly,
                onSelect = vm::showUnreadOnly,
            )
            Spacer(Modifier.height(8.dp))
        }
        when {
            state.loading -> SkeletonRows(4, Modifier.padding(16.dp))
            state.error != null -> MessageBox(state.error!!, onRetry = vm::load)
            state.visible.isEmpty() -> MessageBox(stringResource(R.string.no_notifications))
            else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.visible, key = { it.id }) { n ->
                    val target = notificationTarget(n.kind)
                    NotificationRow(n, target, onClick = {
                        if (!n.read) vm.markRead(n.id)
                        target?.let(onOpen)
                    })
                }
            }
        }
    }
}

/** Unread rows are tinted gold with a dot, read rows are plain cards. */
@Composable
private fun NotificationRow(n: NotificationDto, target: NotificationTarget?, onClick: () -> Unit) {
    val c = Aghour.colors
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (n.read) c.surface else c.gold.copy(alpha = 0.10f))
            .border(1.dp, if (n.read) c.line else c.gold.copy(alpha = 0.40f), shape)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Box(Modifier.size(38.dp).background(c.surface2, CircleShape), contentAlignment = Alignment.Center) {
            Text(notificationSymbol(n.kind))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(n.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = c.ink, modifier = Modifier.weight(1f))
                if (!n.read) Box(Modifier.size(8.dp).background(c.gold, CircleShape))
            }
            Text(n.body, style = MaterialTheme.typography.bodySmall, color = c.muted)
            Row(Modifier.fillMaxWidth().padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                if (target != null) {
                    Text(stringResource(target.labelRes), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = c.goldInk)
                }
                Spacer(Modifier.weight(1f))
                Text(relativeTime(n.createdAt), style = MaterialTheme.typography.labelSmall, color = c.muted)
            }
        }
    }
}

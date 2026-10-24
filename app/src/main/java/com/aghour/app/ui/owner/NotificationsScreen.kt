package com.aghour.app.ui.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.aghour.app.ui.components.MessageBox
import com.aghour.app.ui.components.MutedText
import com.aghour.app.ui.components.SandCard
import com.aghour.app.ui.components.ScreenTitle
import com.aghour.app.ui.components.SkeletonRows
import com.aghour.app.ui.components.appContainer
import com.aghour.app.ui.components.relativeTime
import com.aghour.app.ui.theme.Cream
import com.aghour.app.ui.theme.Forest
import com.aghour.app.ui.theme.Gold
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val loading: Boolean = true,
    val items: List<NotificationDto> = emptyList(),
    val error: String? = null,
)

class NotificationsViewModel(private val owner: OwnerRepository) : ViewModel() {
    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = NotificationsUiState(loading = true)
        viewModelScope.launch {
            _state.value = when (val r = owner.notifications()) {
                is ApiResult.Success -> NotificationsUiState(loading = false, items = r.data.first)
                is ApiResult.Failure -> NotificationsUiState(loading = false, error = r.message)
            }
        }
    }

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

@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    val container = appContainer()
    val vm: NotificationsViewModel = viewModel { NotificationsViewModel(container.owner) }
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().background(Cream)) {
        ScreenTitle(stringResource(R.string.notifications), onBack = onBack, actions = {
            if (state.items.any { !it.read }) {
                TextButton(onClick = vm::markAllRead) { Text(stringResource(R.string.mark_all_read), color = Gold) }
            }
        })
        when {
            state.loading -> SkeletonRows(4, Modifier.padding(16.dp))
            state.error != null -> MessageBox(state.error!!, onRetry = vm::load)
            state.items.isEmpty() -> MessageBox(stringResource(R.string.no_notifications))
            else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.items, key = { it.id }) { n ->
                    SandCard(onClick = { if (!n.read) vm.markRead(n.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!n.read) {
                                Box(Modifier.size(8.dp).background(Gold, CircleShape))
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(n.title, style = MaterialTheme.typography.titleSmall, color = Forest, modifier = Modifier.weight(1f))
                            MutedText(relativeTime(n.createdAt))
                        }
                        Text(n.body, style = MaterialTheme.typography.bodySmall, color = Forest.copy(alpha = 0.8f))
                    }
                }
            }
        }
    }
}

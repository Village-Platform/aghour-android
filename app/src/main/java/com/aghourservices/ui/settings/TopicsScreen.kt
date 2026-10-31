package com.aghourservices.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aghourservices.R
import com.aghourservices.data.remote.ApiResult
import com.aghourservices.data.remote.TopicDto
import com.aghourservices.data.repository.DeviceRepository
import com.aghourservices.ui.components.MessageBox
import com.aghourservices.ui.components.MutedText
import com.aghourservices.ui.components.ScreenTitle
import com.aghourservices.ui.components.SkeletonRows
import com.aghourservices.ui.components.SurfaceCard
import com.aghourservices.ui.components.appContainer
import com.aghourservices.ui.theme.Aghour
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TopicsUiState(
    val loading: Boolean = true,
    val topics: List<TopicDto> = emptyList(),
    val error: String? = null,
)

class TopicsViewModel(private val deviceRepo: DeviceRepository) : ViewModel() {
    private val _state = MutableStateFlow(TopicsUiState())
    val state: StateFlow<TopicsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = deviceRepo.topics()) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, topics = r.data) }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = r.message) }
            }
        }
    }

    fun toggleTopic(topic: TopicDto) {
        viewModelScope.launch {
            val result = if (topic.subscribed) {
                deviceRepo.unsubscribeTopic(topic.key)
            } else {
                deviceRepo.subscribeTopic(topic.key)
            }
            if (result is ApiResult.Success) {
                val updated = result.data
                _state.update { s ->
                    s.copy(topics = s.topics.map { if (it.key == updated.key) updated else it })
                }
            }
        }
    }
}

@Composable
fun TopicsScreen(onBack: () -> Unit) {
    val container = appContainer()
    val vm: TopicsViewModel = viewModel { TopicsViewModel(container.device) }
    val state by vm.state.collectAsStateWithLifecycle()
    val c = Aghour.colors

    Column(Modifier.fillMaxSize().background(c.page)) {
        ScreenTitle(stringResource(R.string.manage_topics), onBack = onBack)
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            MutedText(stringResource(R.string.topics_subtitle))
        }
        Spacer(Modifier.height(8.dp))
        when {
            state.loading -> SkeletonRows(3, Modifier.padding(16.dp))
            state.error != null -> MessageBox(state.error!!, onRetry = vm::load)
            state.topics.isEmpty() -> MessageBox(stringResource(R.string.no_topics))
            else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.topics, key = { it.key }) { topic ->
                    SurfaceCard(padding = 16.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Notifications, contentDescription = null, tint = c.goldInk)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(topic.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = c.ink)
                                topic.description?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall, color = c.muted)
                                }
                            }
                            Switch(
                                checked = topic.subscribed,
                                onCheckedChange = { vm.toggleTopic(topic) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = c.surface,
                                    checkedTrackColor = c.gold,
                                    uncheckedThumbColor = c.surface,
                                    uncheckedTrackColor = c.line,
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

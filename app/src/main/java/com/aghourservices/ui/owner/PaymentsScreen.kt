package com.aghourservices.ui.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.aghourservices.data.remote.SubscriptionDto
import com.aghourservices.data.repository.OwnerRepository
import com.aghourservices.ui.components.Badge
import com.aghourservices.ui.components.MessageBox
import com.aghourservices.ui.components.MutedText
import com.aghourservices.ui.components.ScreenTitle
import com.aghourservices.ui.components.SkeletonRows
import com.aghourservices.ui.components.SurfaceCard
import com.aghourservices.ui.components.appContainer
import com.aghourservices.ui.components.egp
import com.aghourservices.ui.components.formatDate
import com.aghourservices.ui.components.paymentMethodLabel
import com.aghourservices.ui.components.subscriptionState
import com.aghourservices.ui.theme.Aghour
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PaymentsUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val items: List<SubscriptionDto> = emptyList(),
    val page: Int = 0,
    val totalPages: Int = 1,
    val error: String? = null,
) {
    val canLoadMore: Boolean get() = page < totalPages && !loading && !loadingMore && !refreshing
}

class PaymentsViewModel(private val owner: OwnerRepository) : ViewModel() {
    private val _state = MutableStateFlow(PaymentsUiState())
    val state: StateFlow<PaymentsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, error = null, page = 0, totalPages = 1) }
        viewModelScope.launch { loadPage(1) }
    }

    fun refresh() {
        _state.update { it.copy(refreshing = true, error = null) }
        viewModelScope.launch { loadPage(1) }
    }

    fun loadMore() {
        val current = _state.value
        if (!current.canLoadMore) return
        _state.update { it.copy(loadingMore = true) }
        viewModelScope.launch { loadPage(current.page + 1) }
    }

    private suspend fun loadPage(page: Int) {
        when (val r = owner.subscriptions(page = page)) {
            is ApiResult.Success -> _state.update {
                it.copy(
                    loading = false,
                    refreshing = false,
                    loadingMore = false,
                    items = if (page == 1) r.data.data else it.items + r.data.data,
                    page = r.data.meta.page,
                    totalPages = r.data.meta.totalPages,
                )
            }
            is ApiResult.Failure -> _state.update {
                it.copy(loading = false, refreshing = false, loadingMore = false, error = r.message)
            }
        }
    }
}

/** "سجل المدفوعات": every subscription request with its review state. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentsScreen(onBack: () -> Unit) {
    val container = appContainer()
    val vm: PaymentsViewModel = viewModel { PaymentsViewModel(container.owner) }
    val state by vm.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val c = Aghour.colors

    val reachedEnd by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= listState.layoutInfo.totalItemsCount - 3
        }
    }
    LaunchedEffect(reachedEnd, state.canLoadMore) { if (reachedEnd && state.canLoadMore) vm.loadMore() }

    Column(Modifier.fillMaxSize().background(c.page)) {
        ScreenTitle(stringResource(R.string.payments_history), onBack = onBack)
        PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = vm::refresh, modifier = Modifier.fillMaxSize()) {
            when {
                state.loading -> SkeletonRows(4, Modifier.padding(16.dp))
                state.error != null && state.items.isEmpty() -> MessageBox(state.error!!, onRetry = vm::load)
                state.items.isEmpty() -> MessageBox(stringResource(R.string.no_payments))
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.items, key = { it.id }) { s ->
                        val (label, tone) = subscriptionState(s.state)
                        SurfaceCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(s.plan.name, style = MaterialTheme.typography.titleSmall, color = c.ink, modifier = Modifier.weight(1f))
                                Badge(label, tone)
                            }
                            Text(egp(s.amountCents), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = c.ink)
                            MutedText("${paymentMethodLabel(s.paymentMethod)} · ${stringResource(R.string.reference, "⁦${s.transactionReference}⁩")}")
                            MutedText(
                                listOfNotNull(
                                    formatDate(s.createdAt).takeIf { it.isNotEmpty() },
                                    s.endsAt?.let { "حتى ${formatDate(it)}" },
                                ).joinToString(" · "),
                            )
                            s.rejectionReason?.let { Text(it, color = c.bad, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                    if (state.loadingMore) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(28.dp), color = c.gold)
                            }
                        }
                    }
                }
            }
        }
    }
}

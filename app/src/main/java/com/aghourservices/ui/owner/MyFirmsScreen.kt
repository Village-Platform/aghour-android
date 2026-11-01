package com.aghourservices.ui.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aghourservices.R
import com.aghourservices.data.remote.ApiResult
import com.aghourservices.data.remote.CategoryDto
import com.aghourservices.data.remote.FirmDto
import com.aghourservices.data.repository.CatalogRepository
import com.aghourservices.data.repository.OwnerRepository
import com.aghourservices.ui.components.Badge
import com.aghourservices.ui.components.BadgeTone
import com.aghourservices.ui.components.FirmRow
import com.aghourservices.ui.components.MessageBox
import com.aghourservices.ui.components.ScreenTitle
import com.aghourservices.ui.components.SkeletonRows
import com.aghourservices.ui.components.appContainer
import com.aghourservices.ui.components.firmIconUrl
import com.aghourservices.ui.components.firmStatusLabel
import com.aghourservices.ui.theme.Aghour
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyFirmsUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val firms: List<FirmDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val page: Int = 0,
    val totalPages: Int = 1,
    val error: String? = null,
) {
    val canLoadMore: Boolean get() = page < totalPages && !loading && !loadingMore && !refreshing
}

class MyFirmsViewModel(
    private val owner: OwnerRepository,
    private val catalog: CatalogRepository,
    changes: StateFlow<Int>,
) : ViewModel() {
    private val _state = MutableStateFlow(MyFirmsUiState())
    val state: StateFlow<MyFirmsUiState> = _state.asStateFlow()

    init {
        reload()
        // A firm was added, edited or deleted (AdminRepository.changes).
        viewModelScope.launch { changes.drop(1).collect { reload() } }
    }

    fun reload() {
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
        val categories = (catalog.categories() as? ApiResult.Success)?.data.orEmpty()
        when (val r = owner.myFirms(page = page)) {
            is ApiResult.Success -> _state.update {
                it.copy(
                    loading = false,
                    refreshing = false,
                    loadingMore = false,
                    firms = if (page == 1) r.data.data else it.firms + r.data.data,
                    categories = categories,
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

/**
 * The owner's own firms, including pending/suspended ones (with a status badge).
 * [onAddFirm] is null for users who cannot add firms (decided in AppNavigation).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyFirmsScreen(onBack: () -> Unit, onFirmClick: (FirmDto) -> Unit, onAddFirm: (() -> Unit)? = null) {
    val container = appContainer()
    val vm: MyFirmsViewModel = viewModel { MyFirmsViewModel(container.owner, container.catalog, container.admin.changes) }
    val state by vm.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val reachedEnd by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= listState.layoutInfo.totalItemsCount - 3
        }
    }
    LaunchedEffect(reachedEnd, state.canLoadMore) { if (reachedEnd && state.canLoadMore) vm.loadMore() }

    Column(Modifier.fillMaxSize().background(Aghour.colors.page)) {
        ScreenTitle(stringResource(R.string.my_firms), onBack = onBack) {
            if (onAddFirm != null) {
                IconButton(onClick = onAddFirm) { Icon(Icons.Filled.Add, stringResource(R.string.add_firm), tint = Aghour.colors.goldInk) }
            }
        }
        PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = vm::refresh, modifier = Modifier.fillMaxSize()) {
            when {
                state.loading -> SkeletonRows(3, Modifier.padding(16.dp))
                state.error != null && state.firms.isEmpty() -> MessageBox(state.error!!, onRetry = vm::reload)
                state.firms.isEmpty() -> MessageBox(stringResource(R.string.no_my_firms))
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.firms, key = { it.id }) { firm ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            val tone = when (firm.status) {
                                "pending" -> BadgeTone.Gold
                                "suspended" -> BadgeTone.Red
                                else -> BadgeTone.Green
                            }
                            Badge(firmStatusLabel(firm.status), tone)
                            FirmRow(firm, firmIconUrl(firm, state.categories), onClick = { onFirmClick(firm) })
                        }
                    }
                    if (state.loadingMore) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(28.dp), color = Aghour.colors.gold)
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.aghour.app.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aghour.app.R
import com.aghour.app.data.remote.FirmDto
import com.aghour.app.ui.components.FilterChipsRow
import com.aghour.app.ui.components.FirmRow
import com.aghour.app.ui.components.MessageBox
import com.aghour.app.ui.components.MutedText
import com.aghour.app.ui.components.SkeletonRows
import com.aghour.app.ui.components.StatusBarIcons
import com.aghour.app.ui.components.appContainer
import com.aghour.app.ui.components.categoryIcon
import com.aghour.app.ui.components.firmIcon
import com.aghour.app.ui.theme.Aghour

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(onFirmClick: (FirmDto) -> Unit) {
    val container = appContainer()
    val vm: ExploreViewModel = viewModel { ExploreViewModel(container.catalog, container.exploreCategory) }
    val state by vm.state.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current
    val listState = rememberLazyListState()
    val c = Aghour.colors
    StatusBarIcons()

    val reachedEnd by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= listState.layoutInfo.totalItemsCount - 3
        }
    }
    LaunchedEffect(reachedEnd, state.canLoadMore) { if (reachedEnd && state.canLoadMore) vm.loadMore() }

    Column(Modifier.fillMaxSize().background(c.page)) {
        Column(Modifier.statusBarsPadding().padding(top = 12.dp, bottom = 8.dp)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = vm::onQueryChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                placeholder = { Text(stringResource(R.string.explore_hint), color = c.muted) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = c.muted) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { vm.onQueryChange("") }) { Icon(Icons.Filled.Clear, contentDescription = null, tint = c.muted) }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = c.surface2,
                    unfocusedContainerColor = c.surface2,
                    focusedTextColor = c.ink,
                    unfocusedTextColor = c.ink,
                    focusedBorderColor = c.gold,
                    unfocusedBorderColor = c.line,
                    cursorColor = c.gold,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focus.clearFocus(); vm.reload() }),
            )
            Spacer(Modifier.height(12.dp))
            val allLabel = stringResource(R.string.all)
            FilterChipsRow(
                options = listOf<Pair<Long?, String>>(null to allLabel) + state.categories.map { it.id to it.name },
                selected = state.categoryId,
                onSelect = vm::selectCategory,
                icons = mapOf<Long?, ImageVector?>(null to Icons.Outlined.Apps) + state.categories.associate { it.id to categoryIcon(it) },
            )
            if (!state.loading && state.error == null) {
                MutedText(stringResource(R.string.results_count, state.total), Modifier.padding(start = 18.dp, top = 8.dp))
            }
        }

        PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = vm::refresh, modifier = Modifier.fillMaxSize()) {
            when {
                state.loading -> SkeletonRows(modifier = Modifier.padding(16.dp))
                state.error != null && state.firms.isEmpty() -> MessageBox(state.error!!, onRetry = vm::reload)
                state.firms.isEmpty() -> MessageBox(stringResource(R.string.no_firms))
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.firms, key = { it.id }) { firm ->
                        FirmRow(firm, onClick = { onFirmClick(firm) }, icon = firmIcon(firm, state.categories))
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

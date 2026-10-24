package com.aghour.app.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aghour.app.R
import com.aghour.app.data.remote.ApiResult
import com.aghour.app.data.remote.FirmDto
import com.aghour.app.data.repository.CatalogRepository
import com.aghour.app.ui.components.BrandHeader
import com.aghour.app.ui.components.FirmCard
import com.aghour.app.ui.components.MessageBox
import com.aghour.app.ui.components.SkeletonCards
import com.aghour.app.ui.components.appContainer
import com.aghour.app.ui.theme.Cream
import com.aghour.app.ui.theme.Gold500
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val results: List<FirmDto> = emptyList(),
    val searching: Boolean = false,
    val searched: Boolean = false,
    val error: String? = null,
)

/** Searches all visible firms; waits until typing pauses so we don't call the API on every key. */
@OptIn(FlowPreview::class)
class SearchViewModel(private val catalog: CatalogRepository) : ViewModel() {
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()
    private val queries = MutableStateFlow("")

    init {
        viewModelScope.launch {
            queries
                .debounce(400)
                .distinctUntilChanged()
                .collectLatest { query -> search(query) }
        }
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        queries.value = query.trim()
    }

    fun searchNow() = viewModelScope.launch { search(_state.value.query.trim()) }

    private suspend fun search(query: String) {
        if (query.length < 2) {
            _state.update { it.copy(results = emptyList(), searching = false, searched = false, error = null) }
            return
        }
        _state.update { it.copy(searching = true, error = null) }
        when (val result = catalog.searchFirms(query, page = 1)) {
            is ApiResult.Success -> _state.update { it.copy(results = result.data.data, searching = false, searched = true) }
            is ApiResult.Failure -> _state.update { it.copy(error = result.message, searching = false, searched = true) }
        }
    }
}

@Composable
fun SearchScreen(onBack: () -> Unit, onFirmClick: (FirmDto) -> Unit) {
    val container = appContainer()
    val vm: SearchViewModel = viewModel { SearchViewModel(container.catalog) }
    val state by vm.state.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val focus = LocalFocusManager.current

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(Modifier.fillMaxSize()) {
        BrandHeader(showPhoto = false) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = Cream)
                }
                OutlinedTextField(
                    value = state.query,
                    onValueChange = vm::onQueryChange,
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Gold500) },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { vm.onQueryChange("") }) { Icon(Icons.Filled.Clear, contentDescription = null) }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(26.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Gold500,
                        unfocusedBorderColor = Color.Transparent,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        focus.clearFocus()
                        vm.searchNow()
                    }),
                )
            }
        }

        when {
            state.searching -> SkeletonCards(modifier = Modifier.padding(top = 16.dp))
            state.error != null -> MessageBox(state.error!!, onRetry = { vm.searchNow() })
            !state.searched -> MessageBox(stringResource(R.string.search_start))
            state.results.isEmpty() -> MessageBox(stringResource(R.string.search_no_results, state.query.trim()))
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.results, key = { it.id }) { firm ->
                    FirmCard(firm, onClick = { onFirmClick(firm) }, showCategory = true)
                }
            }
        }
    }
}

package com.aghourservices.ui.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import kotlinx.coroutines.launch

data class MyFirmsUiState(
    val loading: Boolean = true,
    val firms: List<FirmDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val error: String? = null,
)

class MyFirmsViewModel(
    private val owner: OwnerRepository,
    private val catalog: CatalogRepository,
    changes: StateFlow<Int>,
) : ViewModel() {
    private val _state = MutableStateFlow(MyFirmsUiState())
    val state: StateFlow<MyFirmsUiState> = _state.asStateFlow()

    init {
        load()
        // A firm was added, edited or deleted (AdminRepository.changes).
        viewModelScope.launch { changes.drop(1).collect { load() } }
    }

    fun load() {
        _state.value = MyFirmsUiState(loading = true)
        viewModelScope.launch {
            val categories = (catalog.categories() as? ApiResult.Success)?.data.orEmpty() // only for icons
            _state.value = when (val r = owner.myFirms()) {
                is ApiResult.Success -> MyFirmsUiState(loading = false, firms = r.data, categories = categories)
                is ApiResult.Failure -> MyFirmsUiState(loading = false, error = r.message)
            }
        }
    }
}

/**
 * The owner's own firms, including pending/suspended ones (with a status badge).
 * [onAddFirm] is null for users who cannot add firms (decided in AppNavigation).
 */
@Composable
fun MyFirmsScreen(onBack: () -> Unit, onFirmClick: (FirmDto) -> Unit, onAddFirm: (() -> Unit)? = null) {
    val container = appContainer()
    val vm: MyFirmsViewModel = viewModel { MyFirmsViewModel(container.owner, container.catalog, container.admin.changes) }
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().background(Aghour.colors.page)) {
        ScreenTitle(stringResource(R.string.my_firms), onBack = onBack) {
            if (onAddFirm != null) {
                IconButton(onClick = onAddFirm) { Icon(Icons.Filled.Add, stringResource(R.string.add_firm), tint = Aghour.colors.goldInk) }
            }
        }
        when {
            state.loading -> SkeletonRows(3, Modifier.padding(16.dp))
            state.error != null -> MessageBox(state.error!!, onRetry = vm::load)
            state.firms.isEmpty() -> MessageBox(stringResource(R.string.no_my_firms))
            else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
            }
        }
    }
}

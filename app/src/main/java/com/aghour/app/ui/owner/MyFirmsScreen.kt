package com.aghour.app.ui.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aghour.app.R
import com.aghour.app.data.remote.ApiResult
import com.aghour.app.data.remote.FirmDto
import com.aghour.app.data.repository.OwnerRepository
import com.aghour.app.ui.components.Badge
import com.aghour.app.ui.components.BadgeTone
import com.aghour.app.ui.components.FirmRow
import com.aghour.app.ui.components.MessageBox
import com.aghour.app.ui.components.ScreenTitle
import com.aghour.app.ui.components.SkeletonRows
import com.aghour.app.ui.components.appContainer
import com.aghour.app.ui.theme.Aghour
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MyFirmsUiState(val loading: Boolean = true, val firms: List<FirmDto> = emptyList(), val error: String? = null)

class MyFirmsViewModel(private val owner: OwnerRepository) : ViewModel() {
    private val _state = MutableStateFlow(MyFirmsUiState())
    val state: StateFlow<MyFirmsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = MyFirmsUiState(loading = true)
        viewModelScope.launch {
            _state.value = when (val r = owner.myFirms()) {
                is ApiResult.Success -> MyFirmsUiState(loading = false, firms = r.data)
                is ApiResult.Failure -> MyFirmsUiState(loading = false, error = r.message)
            }
        }
    }
}

/** The owner's own firms, including pending/suspended ones (with a status badge). */
@Composable
fun MyFirmsScreen(onBack: () -> Unit, onFirmClick: (FirmDto) -> Unit) {
    val container = appContainer()
    val vm: MyFirmsViewModel = viewModel { MyFirmsViewModel(container.owner) }
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().background(Aghour.colors.page)) {
        ScreenTitle(stringResource(R.string.my_firms), onBack = onBack)
        when {
            state.loading -> SkeletonRows(3, Modifier.padding(16.dp))
            state.error != null -> MessageBox(state.error!!, onRetry = vm::load)
            state.firms.isEmpty() -> MessageBox(stringResource(R.string.no_firms))
            else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.firms, key = { it.id }) { firm ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        when (firm.status) {
                            "pending" -> Badge("قيد المراجعة", BadgeTone.Gold)
                            "suspended" -> Badge("موقوف", BadgeTone.Red)
                            else -> Badge("نشط")
                        }
                        FirmRow(firm, onClick = { onFirmClick(firm) })
                    }
                }
            }
        }
    }
}

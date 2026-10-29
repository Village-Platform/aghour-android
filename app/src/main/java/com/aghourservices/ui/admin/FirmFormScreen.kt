package com.aghourservices.ui.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aghourservices.R
import com.aghourservices.data.remote.ApiResult
import com.aghourservices.data.remote.CategoryDto
import com.aghourservices.data.remote.FirmDto
import com.aghourservices.data.repository.AdminRepository
import com.aghourservices.data.repository.CatalogRepository
import com.aghourservices.data.repository.FirmInput
import com.aghourservices.ui.components.ErrorBanner
import com.aghourservices.ui.components.FilterChipsRow
import com.aghourservices.ui.components.FormField
import com.aghourservices.ui.components.GoldButton
import com.aghourservices.ui.components.MessageBox
import com.aghourservices.ui.components.ScreenTitle
import com.aghourservices.ui.components.Segmented
import com.aghourservices.ui.components.SkeletonRows
import com.aghourservices.ui.components.appContainer
import com.aghourservices.ui.components.categoryIconUrl
import com.aghourservices.ui.theme.Aghour
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Statuses the server accepts (Firm#status). New firms made by the super admin start active. */
val FirmStatuses = listOf("active", "pending", "suspended")

data class FirmFormUiState(
    val loading: Boolean = true,
    val loadError: String? = null,
    val categories: List<CategoryDto> = emptyList(),
    val categoryId: Long? = null,
    val name: String = "",
    val description: String = "",
    val phone: String = "",
    val whatsapp: String = "",
    val address: String = "",
    val latitude: String = "",
    val longitude: String = "",
    val workingHours: String = "",
    val status: String = "active",
    val nameError: Int? = null,
    val categoryError: Int? = null,
    val coordinatesError: Int? = null,
    val submitting: Boolean = false,
    val error: String? = null,
    /** Set once saved; the screen then closes. */
    val savedId: Long? = null,
)

/** Create ([firmId] null) or edit a firm. */
class FirmFormViewModel(
    private val catalog: CatalogRepository,
    private val admin: AdminRepository,
    private val firmId: Long?,
) : ViewModel() {
    private val _state = MutableStateFlow(FirmFormUiState())
    val state: StateFlow<FirmFormUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, loadError = null) }
        viewModelScope.launch {
            val categories = when (val r = catalog.categories()) {
                is ApiResult.Success -> r.data
                is ApiResult.Failure -> return@launch _state.update { it.copy(loading = false, loadError = r.message) }
            }
            if (firmId == null) {
                _state.update { it.copy(loading = false, categories = categories) }
                return@launch
            }
            when (val r = catalog.firm(firmId)) {
                is ApiResult.Success -> _state.update { it.filledFrom(r.data).copy(loading = false, categories = categories) }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, loadError = r.message) }
            }
        }
    }

    fun update(transform: FirmFormUiState.() -> FirmFormUiState) =
        _state.update { it.transform().copy(error = null, nameError = null, categoryError = null, coordinatesError = null) }

    fun submit() {
        val s = _state.value
        if (s.submitting) return
        val latitude = s.latitude.trim().toDoubleOrNull()
        val longitude = s.longitude.trim().toDoubleOrNull()
        // Both empty, or both valid numbers.
        val coordinatesOk = (s.latitude.isBlank() && s.longitude.isBlank()) || (latitude != null && longitude != null)
        val nameError = if (s.name.isBlank()) R.string.error_required else null
        val categoryError = if (s.categoryId == null) R.string.error_category_required else null
        val coordinatesError = if (coordinatesOk) null else R.string.error_coordinates
        if (nameError != null || categoryError != null || coordinatesError != null || s.categoryId == null) {
            _state.update { it.copy(nameError = nameError, categoryError = categoryError, coordinatesError = coordinatesError) }
            return
        }
        val input = FirmInput(
            categoryId = s.categoryId,
            name = s.name,
            description = s.description,
            phone = s.phone,
            whatsapp = s.whatsapp,
            address = s.address,
            latitude = latitude,
            longitude = longitude,
            workingHours = s.workingHours,
            status = s.status,
        )
        _state.update { it.copy(submitting = true) }
        viewModelScope.launch {
            val result = if (firmId == null) admin.createFirm(input) else admin.updateFirm(firmId, input)
            when (result) {
                is ApiResult.Success -> _state.update { it.copy(submitting = false, savedId = result.data.id) }
                is ApiResult.Failure -> _state.update { it.copy(submitting = false, error = result.message) }
            }
        }
    }
}

private fun FirmFormUiState.filledFrom(firm: FirmDto) = copy(
    categoryId = firm.category.id,
    name = firm.name,
    description = firm.description.orEmpty(),
    phone = firm.phone.orEmpty(),
    whatsapp = firm.whatsapp.orEmpty(),
    address = firm.address.orEmpty(),
    latitude = firm.latitude?.toString().orEmpty(),
    longitude = firm.longitude?.toString().orEmpty(),
    workingHours = firm.workingHours.orEmpty(),
    status = firm.status,
)

@Composable
fun firmStatusLabel(status: String): String = when (status) {
    "active" -> stringResource(R.string.firm_status_active)
    "pending" -> stringResource(R.string.firm_status_pending)
    "suspended" -> stringResource(R.string.firm_status_suspended)
    else -> status
}

@Composable
fun FirmFormScreen(firmId: Long?, onBack: () -> Unit, onSaved: (Long) -> Unit) {
    val container = appContainer()
    val vm: FirmFormViewModel = viewModel { FirmFormViewModel(container.catalog, container.admin, firmId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val saved = stringResource(R.string.firm_saved)
    val c = Aghour.colors

    LaunchedEffect(state.savedId) {
        state.savedId?.let {
            Toast.makeText(context, saved, Toast.LENGTH_SHORT).show()
            onSaved(it)
        }
    }

    Column(Modifier.fillMaxSize().background(c.page).imePadding()) {
        ScreenTitle(stringResource(if (firmId == null) R.string.add_firm else R.string.edit_firm), onBack = onBack)
        when {
            state.loading -> SkeletonRows(4, Modifier.padding(16.dp))
            state.loadError != null -> MessageBox(state.loadError ?: "", onRetry = vm::load)
            else -> Column(
                Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FormField(
                    label = stringResource(R.string.firm_name),
                    value = state.name,
                    onChange = { v -> vm.update { copy(name = v) } },
                    error = state.nameError,
                )

                Text(stringResource(R.string.firm_category), style = MaterialTheme.typography.labelMedium, color = c.muted)
                FilterChipsRow(
                    options = state.categories.map { it.id to it.name },
                    selected = state.categoryId,
                    onSelect = { id -> vm.update { copy(categoryId = id) } },
                    contentPadding = PaddingValues(0.dp),
                    imageUrls = state.categories.mapNotNull { cat -> categoryIconUrl(cat.iconUrl)?.let { cat.id to it } }.toMap(),
                )
                state.categoryError?.let { Text(stringResource(it), color = c.bad, style = MaterialTheme.typography.bodySmall) }

                Text(stringResource(R.string.firm_status), style = MaterialTheme.typography.labelMedium, color = c.muted)
                Row {
                    Segmented(
                        options = FirmStatuses.map { it to firmStatusLabel(it) },
                        selected = state.status,
                        onSelect = { s -> vm.update { copy(status = s) } },
                    )
                }

                FormField(
                    label = stringResource(R.string.phone),
                    value = state.phone,
                    onChange = { v -> vm.update { copy(phone = v) } },
                    keyboardType = KeyboardType.Phone,
                    ltr = true,
                )
                FormField(
                    label = stringResource(R.string.whatsapp),
                    value = state.whatsapp,
                    onChange = { v -> vm.update { copy(whatsapp = v) } },
                    keyboardType = KeyboardType.Phone,
                    ltr = true,
                )
                FormField(
                    label = stringResource(R.string.address),
                    value = state.address,
                    onChange = { v -> vm.update { copy(address = v) } },
                )
                FormField(
                    label = stringResource(R.string.working_hours),
                    value = state.workingHours,
                    onChange = { v -> vm.update { copy(workingHours = v) } },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FormField(
                        label = stringResource(R.string.latitude),
                        value = state.latitude,
                        onChange = { v -> vm.update { copy(latitude = v) } },
                        modifier = Modifier.weight(1f),
                        keyboardType = KeyboardType.Decimal,
                        ltr = true,
                    )
                    FormField(
                        label = stringResource(R.string.longitude),
                        value = state.longitude,
                        onChange = { v -> vm.update { copy(longitude = v) } },
                        modifier = Modifier.weight(1f),
                        keyboardType = KeyboardType.Decimal,
                        ltr = true,
                    )
                }
                state.coordinatesError?.let { Text(stringResource(it), color = c.bad, style = MaterialTheme.typography.bodySmall) }
                FormField(
                    label = stringResource(R.string.description),
                    value = state.description,
                    onChange = { v -> vm.update { copy(description = v) } },
                    imeAction = ImeAction.Default,
                    multiline = true,
                )

                ErrorBanner(state.error)
                GoldButton(stringResource(R.string.save), vm::submit, loading = state.submitting)
            }
        }
    }
}

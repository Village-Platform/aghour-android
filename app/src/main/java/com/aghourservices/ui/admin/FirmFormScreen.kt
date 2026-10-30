package com.aghourservices.ui.admin

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import com.aghourservices.ui.components.FilterChipsRow
import com.aghourservices.ui.components.FormField
import com.aghourservices.ui.components.LocationPicker
import com.aghourservices.ui.components.MutedText
import com.aghourservices.ui.components.Segmented
import com.aghourservices.ui.components.appContainer
import com.aghourservices.ui.components.categoryIconUrl
import com.aghourservices.ui.components.firmStatusLabel
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
    /** The owner's edit sent an approved firm back for review (the server set it to pending). */
    val sentForReview: Boolean = false,
)

/** Create ([firmId] null) or edit a firm. [canSetStatus]: super admin only; owners never send a status. */
class FirmFormViewModel(
    private val catalog: CatalogRepository,
    private val admin: AdminRepository,
    private val firmId: Long?,
    private val canSetStatus: Boolean,
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
            status = s.status.takeIf { canSetStatus },
        )
        _state.update { it.copy(submitting = true) }
        viewModelScope.launch {
            val result = if (firmId == null) admin.createFirm(input) else admin.updateFirm(firmId, input)
            when (result) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        submitting = false,
                        savedId = result.data.id,
                        sentForReview = s.status == "active" && result.data.status == "pending",
                    )
                }
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
fun FirmFormScreen(firmId: Long?, canSetStatus: Boolean, onBack: () -> Unit, onSaved: (Long) -> Unit) {
    val container = appContainer()
    val vm: FirmFormViewModel = viewModel { FirmFormViewModel(container.catalog, container.admin, firmId, canSetStatus) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val saved = stringResource(R.string.firm_saved)
    val sentForReview = stringResource(R.string.firm_sent_for_review)

    LaunchedEffect(state.savedId) {
        state.savedId?.let {
            Toast.makeText(context, if (state.sentForReview) sentForReview else saved, Toast.LENGTH_LONG).show()
            onSaved(it)
        }
    }

    AdminFormScaffold(
        title = stringResource(if (firmId == null) R.string.add_firm else R.string.edit_firm),
        onBack = onBack,
        loading = state.loading,
        loadError = state.loadError,
        onRetry = vm::load,
        error = state.error,
        submitting = state.submitting,
        onSave = vm::submit,
    ) {
        FormField(
            label = stringResource(R.string.firm_name),
            value = state.name,
            onChange = { v -> vm.update { copy(name = v) } },
            error = state.nameError,
        )

        FieldLabel(stringResource(R.string.firm_category))
        FilterChipsRow(
            options = state.categories.map { it.id to it.name },
            selected = state.categoryId,
            onSelect = { id -> vm.update { copy(categoryId = id) } },
            contentPadding = PaddingValues(0.dp),
            imageUrls = state.categories.mapNotNull { cat -> categoryIconUrl(cat.iconUrl)?.let { cat.id to it } }.toMap(),
        )
        FieldError(state.categoryError)

        if (canSetStatus) {
            FieldLabel(stringResource(R.string.firm_status))
            Row {
                Segmented(
                    options = FirmStatuses.map { it to firmStatusLabel(it) },
                    selected = state.status,
                    onSelect = { s -> vm.update { copy(status = s) } },
                )
            }
        } else if (firmId == null) {
            // Owners: a new firm waits for the super admin's approval.
            MutedText(stringResource(R.string.firm_review_note))
        } else if (state.status == "active") {
            // Owners: the server sends an edited approved firm back for review (Firms::Update).
            MutedText(stringResource(R.string.firm_edit_review_note))
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
        LocationPicker(
            latitude = state.latitude,
            longitude = state.longitude,
            onCoordinatesChanged = { lat, lng ->
                vm.update { copy(latitude = lat, longitude = lng) }
            },
            error = state.coordinatesError,
        )
        FormField(
            label = stringResource(R.string.description),
            value = state.description,
            onChange = { v -> vm.update { copy(description = v) } },
            imeAction = ImeAction.Default,
            multiline = true,
        )
    }
}

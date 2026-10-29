package com.aghourservices.ui.admin

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
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
import com.aghourservices.data.repository.AdminRepository
import com.aghourservices.data.repository.PlanInput
import com.aghourservices.ui.components.FormField
import com.aghourservices.ui.components.SwitchRow
import com.aghourservices.ui.components.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class PlanFormUiState(
    val loading: Boolean = true,
    val loadError: String? = null,
    val name: String = "",
    val description: String = "",
    /** In pounds, as typed ("150" or "150.50"); sent as cents. */
    val price: String = "",
    val durationDays: String = "30",
    val maxFirms: String = "1",
    val position: String = "0",
    val active: Boolean = true,
    val nameError: Int? = null,
    val priceError: Int? = null,
    val durationError: Int? = null,
    val maxFirmsError: Int? = null,
    val positionError: Int? = null,
    val submitting: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
    val deleted: Boolean = false,
)

/** "150.50" -> 15050. BigDecimal, not Double, so 150.10 does not become 15009. null = not a valid amount. */
private fun poundsToCents(text: String): Int? =
    text.trim().toBigDecimalOrNull()
        ?.takeIf { it.signum() >= 0 && it.scale() <= 2 }
        ?.movePointRight(2)
        ?.runCatching { intValueExact() }
        ?.getOrNull()

private fun centsToPounds(cents: Int): String = BigDecimal(cents).movePointLeft(2).stripTrailingZeros().toPlainString()

class PlanFormViewModel(private val admin: AdminRepository, private val planId: Long?) : ViewModel() {
    private val _state = MutableStateFlow(PlanFormUiState(loading = planId != null))
    val state: StateFlow<PlanFormUiState> = _state.asStateFlow()

    init {
        if (planId != null) load()
    }

    fun load() {
        val id = planId ?: return
        _state.update { it.copy(loading = true, loadError = null) }
        viewModelScope.launch {
            when (val r = admin.plan(id)) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        loading = false,
                        name = r.data.name,
                        description = r.data.description.orEmpty(),
                        price = centsToPounds(r.data.priceCents),
                        durationDays = r.data.durationDays.toString(),
                        maxFirms = r.data.maxFirms.toString(),
                        position = r.data.position.toString(),
                        active = r.data.active,
                    )
                }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, loadError = r.message) }
            }
        }
    }

    fun update(transform: PlanFormUiState.() -> PlanFormUiState) =
        _state.update {
            it.transform().copy(
                error = null, nameError = null, priceError = null, durationError = null, maxFirmsError = null, positionError = null,
            )
        }

    fun submit() {
        val s = _state.value
        if (s.submitting) return
        val price = poundsToCents(s.price)
        val duration = s.durationDays.trim().toIntOrNull()?.takeIf { it > 0 }
        val maxFirms = s.maxFirms.trim().toIntOrNull()?.takeIf { it > 0 }
        val position = s.position.trim().toIntOrNull()?.takeIf { it >= 0 }
        val nameError = if (s.name.isBlank()) R.string.error_required else null
        if (nameError != null || price == null || duration == null || maxFirms == null || position == null) {
            _state.update {
                it.copy(
                    nameError = nameError,
                    priceError = if (price == null) R.string.error_amount else null,
                    durationError = if (duration == null) R.string.error_positive_number else null,
                    maxFirmsError = if (maxFirms == null) R.string.error_positive_number else null,
                    positionError = if (position == null) R.string.error_whole_number else null,
                )
            }
            return
        }
        val input = PlanInput(s.name, s.description, price, duration, maxFirms, position, s.active)
        _state.update { it.copy(submitting = true) }
        viewModelScope.launch {
            val result = if (planId == null) admin.createPlan(input) else admin.updatePlan(planId, input)
            when (result) {
                is ApiResult.Success -> _state.update { it.copy(submitting = false, saved = true) }
                is ApiResult.Failure -> _state.update { it.copy(submitting = false, error = result.message) }
            }
        }
    }

    fun delete() {
        val id = planId ?: return
        if (_state.value.submitting) return
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            when (val r = admin.deletePlan(id)) {
                is ApiResult.Success -> _state.update { it.copy(submitting = false, deleted = true) }
                is ApiResult.Failure -> _state.update { it.copy(submitting = false, error = r.message) }
            }
        }
    }
}

/** Create ([planId] null) or edit a subscription plan. Closes itself when done. */
@Composable
fun PlanFormScreen(planId: Long?, onBack: () -> Unit, onDone: () -> Unit) {
    val container = appContainer()
    val vm: PlanFormViewModel = viewModel { PlanFormViewModel(container.admin, planId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val savedMessage = stringResource(R.string.saved)
    val deletedMessage = stringResource(R.string.deleted)

    LaunchedEffect(state.saved, state.deleted) {
        if (state.saved || state.deleted) {
            Toast.makeText(context, if (state.deleted) deletedMessage else savedMessage, Toast.LENGTH_SHORT).show()
            onDone()
        }
    }

    AdminFormScaffold(
        title = stringResource(if (planId == null) R.string.add_plan else R.string.edit_plan),
        onBack = onBack,
        loading = state.loading,
        loadError = state.loadError,
        onRetry = vm::load,
        error = state.error,
        submitting = state.submitting,
        onSave = vm::submit,
        delete = planId?.let {
            DeleteAction(
                title = stringResource(R.string.delete_plan_title),
                body = stringResource(R.string.delete_plan_body, state.name),
                onDelete = vm::delete,
            )
        },
    ) {
        FormField(
            label = stringResource(R.string.plan_name),
            value = state.name,
            onChange = { v -> vm.update { copy(name = v) } },
            error = state.nameError,
        )
        FormField(
            label = stringResource(R.string.price_egp),
            value = state.price,
            onChange = { v -> vm.update { copy(price = v) } },
            error = state.priceError,
            keyboardType = KeyboardType.Decimal,
            ltr = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FormField(
                label = stringResource(R.string.duration_days),
                value = state.durationDays,
                onChange = { v -> vm.update { copy(durationDays = v) } },
                modifier = Modifier.weight(1f),
                error = state.durationError,
                keyboardType = KeyboardType.Number,
                ltr = true,
            )
            FormField(
                label = stringResource(R.string.max_firms),
                value = state.maxFirms,
                onChange = { v -> vm.update { copy(maxFirms = v) } },
                modifier = Modifier.weight(1f),
                error = state.maxFirmsError,
                keyboardType = KeyboardType.Number,
                ltr = true,
            )
        }
        FormField(
            label = stringResource(R.string.position),
            value = state.position,
            onChange = { v -> vm.update { copy(position = v) } },
            error = state.positionError,
            keyboardType = KeyboardType.Number,
            ltr = true,
        )
        SwitchRow(stringResource(R.string.active), state.active, { v -> vm.update { copy(active = v) } })
        FormField(
            label = stringResource(R.string.plan_features),
            value = state.description,
            onChange = { v -> vm.update { copy(description = v) } },
            imeAction = ImeAction.Default,
            multiline = true,
        )
    }
}

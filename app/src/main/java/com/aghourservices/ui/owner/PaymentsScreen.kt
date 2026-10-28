package com.aghourservices.ui.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
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
import com.aghourservices.data.remote.SubscriptionDto
import com.aghourservices.data.repository.OwnerRepository
import com.aghourservices.ui.components.Badge
import com.aghourservices.ui.components.MessageBox
import com.aghourservices.ui.components.MutedText
import com.aghourservices.ui.components.SurfaceCard
import com.aghourservices.ui.components.ScreenTitle
import com.aghourservices.ui.components.SkeletonRows
import com.aghourservices.ui.components.appContainer
import com.aghourservices.ui.components.egp
import com.aghourservices.ui.components.formatDate
import com.aghourservices.ui.components.paymentMethodLabel
import com.aghourservices.ui.components.subscriptionState
import com.aghourservices.ui.theme.Aghour
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PaymentsUiState(val loading: Boolean = true, val items: List<SubscriptionDto> = emptyList(), val error: String? = null)

class PaymentsViewModel(private val owner: OwnerRepository) : ViewModel() {
    private val _state = MutableStateFlow(PaymentsUiState())
    val state: StateFlow<PaymentsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = PaymentsUiState(loading = true)
        viewModelScope.launch {
            _state.value = when (val r = owner.subscriptions()) {
                is ApiResult.Success -> PaymentsUiState(loading = false, items = r.data)
                is ApiResult.Failure -> PaymentsUiState(loading = false, error = r.message)
            }
        }
    }
}

/** "سجل المدفوعات": every subscription request with its review state. */
@Composable
fun PaymentsScreen(onBack: () -> Unit) {
    val container = appContainer()
    val vm: PaymentsViewModel = viewModel { PaymentsViewModel(container.owner) }
    val state by vm.state.collectAsStateWithLifecycle()
    val c = Aghour.colors

    Column(Modifier.fillMaxSize().background(c.page)) {
        ScreenTitle(stringResource(R.string.payments_history), onBack = onBack)
        when {
            state.loading -> SkeletonRows(4, Modifier.padding(16.dp))
            state.error != null -> MessageBox(state.error!!, onRetry = vm::load)
            state.items.isEmpty() -> MessageBox(stringResource(R.string.no_payments))
            else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                s.endsAt?.let { stringResource(R.string.until_date, formatDate(it)) },
                            ).joinToString(" · "),
                        )
                        s.rejectionReason?.let { Text(it, color = c.bad, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
    }
}

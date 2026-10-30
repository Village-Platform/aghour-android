package com.aghourservices.ui.owner

import com.aghourservices.data.analytics.AnalyticsEvent
import com.aghourservices.data.analytics.AnalyticsService
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aghourservices.R
import com.aghourservices.data.remote.ApiResult
import com.aghourservices.data.remote.PaymentMethodDto
import com.aghourservices.data.remote.SubscriptionPlanDto
import com.aghourservices.data.repository.OwnerRepository
import com.aghourservices.ui.components.ErrorBanner
import com.aghourservices.ui.components.FormField
import com.aghourservices.ui.components.GoldButton
import com.aghourservices.ui.components.MessageBox
import com.aghourservices.ui.components.ScreenTitle
import com.aghourservices.ui.components.SkeletonRows
import com.aghourservices.ui.components.appContainer
import com.aghourservices.ui.components.copyToClipboard
import com.aghourservices.ui.components.egp
import com.aghourservices.ui.theme.Aghour
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PayUiState(
    val loading: Boolean = true,
    val plan: SubscriptionPlanDto? = null,
    val methods: List<PaymentMethodDto> = emptyList(),
    val method: String? = null,
    val reference: String = "",
    val payerPhone: String = "",
    val referenceError: Int? = null,
    val submitting: Boolean = false,
    val error: String? = null,
    val done: Boolean = false,
)

class PayViewModel(
    private val owner: OwnerRepository,
    private val analytics: AnalyticsService,
    private val planId: Long,
) : ViewModel() {
    private val _state = MutableStateFlow(PayUiState())
    val state: StateFlow<PayUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val plans = async { owner.plans() }
            val methods = async { owner.paymentMethods() }
            val plan = (plans.await() as? ApiResult.Success)?.data?.firstOrNull { it.id == planId }
            when (val m = methods.await()) {
                is ApiResult.Success -> _state.update {
                    it.copy(loading = false, plan = plan, methods = m.data, method = m.data.firstOrNull()?.key)
                }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, plan = plan, error = m.message) }
            }
        }
    }

    fun update(transform: PayUiState.() -> PayUiState) = _state.update { it.transform().copy(error = null, referenceError = null) }

    fun submit() {
        val s = _state.value
        val method = s.method ?: return
        if (s.reference.isBlank()) {
            _state.update { it.copy(referenceError = R.string.error_reference_required) }
            return
        }
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            when (val r = owner.subscribe(planId, method, s.reference, s.payerPhone)) {
                is ApiResult.Success -> {
                    analytics.log(AnalyticsEvent.SubmitPayment(planId, method, s.plan?.priceCents))
                    _state.update { it.copy(submitting = false, done = true) }
                }
                is ApiResult.Failure -> _state.update { it.copy(submitting = false, error = r.message) }
            }
        }
    }
}

/** "إتمام الدفع": plan summary, InstaPay / Vodafone Cash details, transfer reference. */
@Composable
fun PayScreen(planId: Long, onBack: () -> Unit, onDone: () -> Unit) {
    val container = appContainer()
    val vm: PayViewModel = viewModel { PayViewModel(container.owner, container.analytics, planId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val copied = stringResource(R.string.copied)
    val c = Aghour.colors

    Column(Modifier.fillMaxSize().background(c.page).imePadding()) {
        ScreenTitle(stringResource(R.string.pay_title), onBack = onBack)
        val plan = state.plan
        when {
            state.loading -> SkeletonRows(3, Modifier.padding(16.dp))
            plan == null -> MessageBox(state.error ?: stringResource(R.string.no_subscription), onRetry = vm::load)
            else -> Column(
                Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Plan summary
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(c.gold.copy(alpha = 0.15f))
                        .border(1.dp, c.gold.copy(alpha = 0.40f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.plan_label), color = c.goldInk, style = MaterialTheme.typography.labelSmall)
                        Text("${plan.name} · ${plan.durationDays} يوم", color = c.ink, style = MaterialTheme.typography.titleSmall)
                    }
                    Text(egp(plan.priceCents), color = c.ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                }

                // Payment methods
                state.methods.forEach { pm ->
                    val selected = pm.key == state.method
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(c.surface)
                            .border(if (selected) 2.dp else 1.dp, if (selected) c.gold else c.line, RoundedCornerShape(16.dp))
                            .clickable { vm.update { copy(method = pm.key) } }
                            .padding(12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (pm.key == "instapay") Icons.Outlined.AccountBalance else Icons.Outlined.PhoneAndroid,
                                contentDescription = null,
                                tint = c.goldInk,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                pm.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = c.ink,
                                modifier = Modifier.weight(1f),
                            )
                            RadioButton(
                                selected = selected,
                                onClick = { vm.update { copy(method = pm.key) } },
                                colors = RadioButtonDefaults.colors(selectedColor = c.goldInk, unselectedColor = c.muted),
                            )
                        }
                        if (selected && pm.account != null) {
                            Column(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.surface2).padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        stringResource(R.string.transfer_to, "⁦${pm.account}⁩"),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = c.ink,
                                        modifier = Modifier.weight(1f),
                                    )
                                    TextButton(onClick = {
                                        context.copyToClipboard(pm.name, pm.account)
                                        Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                                    }) { Text(stringResource(R.string.copy), color = c.goldInk) }
                                }
                                Text(stringResource(R.string.amount, egp(plan.priceCents)), style = MaterialTheme.typography.bodySmall, color = c.ink)
                            }
                        }
                    }
                }

                FormField(
                    label = stringResource(R.string.transfer_reference),
                    value = state.reference,
                    onChange = { v -> vm.update { copy(reference = v) } },
                    placeholder = stringResource(R.string.transfer_reference_hint),
                    error = state.referenceError,
                    ltr = true,
                )
                FormField(
                    label = stringResource(R.string.payer_phone),
                    value = state.payerPhone,
                    onChange = { v -> vm.update { copy(payerPhone = v.filter { it.isDigit() }.take(11)) } },
                    placeholder = "01X XXXX XXXX",
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done,
                    ltr = true,
                )
                ErrorBanner(state.error)
                GoldButton(stringResource(R.string.send_for_review), vm::submit, loading = state.submitting, enabled = state.method != null)
                Text(
                    stringResource(R.string.pay_activation_note),
                    color = c.muted,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (state.done) {
        AlertDialog(
            onDismissRequest = onDone,
            title = { Text(stringResource(R.string.payment_sent_title)) },
            text = { Text(stringResource(R.string.payment_sent_body)) },
            confirmButton = { TextButton(onClick = onDone) { Text(stringResource(R.string.ok), color = c.goldInk) } },
        )
    }
}

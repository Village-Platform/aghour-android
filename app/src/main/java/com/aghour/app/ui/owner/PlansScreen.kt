package com.aghour.app.ui.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aghour.app.R
import com.aghour.app.data.remote.ApiResult
import com.aghour.app.data.remote.SubscriptionPlanDto
import com.aghour.app.data.repository.OwnerRepository
import com.aghour.app.ui.components.Badge
import com.aghour.app.ui.components.BadgeTone
import com.aghour.app.ui.components.GhostButton
import com.aghour.app.ui.components.GoldButton
import com.aghour.app.ui.components.MessageBox
import com.aghour.app.ui.components.ScreenTitle
import com.aghour.app.ui.components.SkeletonRows
import com.aghour.app.ui.components.appContainer
import com.aghour.app.ui.components.egp
import com.aghour.app.ui.theme.Aghour
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlansUiState(val loading: Boolean = true, val plans: List<SubscriptionPlanDto> = emptyList(), val error: String? = null)

class PlansViewModel(private val owner: OwnerRepository) : ViewModel() {
    private val _state = MutableStateFlow(PlansUiState())
    val state: StateFlow<PlansUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = PlansUiState(loading = true)
        viewModelScope.launch {
            _state.value = when (val r = owner.plans()) {
                is ApiResult.Success -> PlansUiState(loading = false, plans = r.data)
                is ApiResult.Failure -> PlansUiState(loading = false, error = r.message)
            }
        }
    }
}

/** "اختر باقتك": the middle plan is highlighted as the most popular (gold card), as in the design. */
@Composable
fun PlansScreen(onBack: () -> Unit, onSubscribe: (SubscriptionPlanDto) -> Unit) {
    val container = appContainer()
    val vm: PlansViewModel = viewModel { PlansViewModel(container.owner) }
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().background(Aghour.colors.page)) {
        ScreenTitle(stringResource(R.string.plans_title), stringResource(R.string.plans_subtitle), onBack = onBack)
        when {
            state.loading -> SkeletonRows(3, Modifier.padding(16.dp))
            state.error != null -> MessageBox(state.error!!, onRetry = vm::load)
            else -> {
                val highlighted = if (state.plans.size >= 3) 1 else state.plans.lastIndex
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(state.plans, key = { _, p -> p.id }) { index, plan ->
                        PlanCard(plan, highlighted = index == highlighted, onSubscribe = { onSubscribe(plan) })
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanCard(plan: SubscriptionPlanDto, highlighted: Boolean, onSubscribe: () -> Unit) {
    val c = Aghour.colors
    val features = buildList {
        add(stringResource(R.string.plan_visibility))
        add(stringResource(R.string.plan_duration, plan.durationDays))
        add(stringResource(R.string.plan_max_firms, plan.maxFirms))
        plan.description?.lines()?.filter { it.isNotBlank() }?.let { addAll(it) }
    }
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (highlighted) c.gold.copy(alpha = 0.10f) else c.surface)
            .border(2.dp, if (highlighted) c.gold else c.line, shape)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                plan.name,
                color = if (highlighted) c.goldInk else c.ink,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (highlighted) Badge(stringResource(R.string.most_popular), BadgeTone.Gold)
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(egp(plan.priceCents), color = c.ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(
                " / ${stringResource(R.string.plan_duration, plan.durationDays)}",
                color = c.muted,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        features.forEach { feature ->
            Text("✓ $feature", color = c.muted, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(12.dp))
        if (highlighted) GoldButton(stringResource(R.string.subscribe), onSubscribe) else GhostButton(stringResource(R.string.subscribe), onSubscribe)
    }
}

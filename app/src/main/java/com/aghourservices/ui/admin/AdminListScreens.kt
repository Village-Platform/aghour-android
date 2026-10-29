package com.aghourservices.ui.admin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aghourservices.R
import com.aghourservices.data.remote.CategoryDto
import com.aghourservices.data.remote.SubscriptionPlanDto
import com.aghourservices.ui.components.appContainer
import com.aghourservices.ui.components.categoryIconUrl
import com.aghourservices.ui.components.egp

/** Every category, inactive ones included (the API returns them to a super admin). */
@Composable
fun CategoriesAdminScreen(onBack: () -> Unit, onAdd: () -> Unit, onEdit: (Long) -> Unit) {
    val container = appContainer()
    val vm: AdminListViewModel<CategoryDto> = viewModel {
        AdminListViewModel({ container.catalog.categories(refresh = true) }, container.admin.changes)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    AdminList(
        title = stringResource(R.string.manage_categories),
        state = state,
        rows = state.items.map {
            AdminRow(
                id = it.id,
                title = it.name,
                subtitle = it.description?.takeIf { d -> d.isNotBlank() },
                active = it.active,
                showIcon = true,
                iconUrl = categoryIconUrl(it.iconUrl),
            )
        },
        onBack = onBack,
        onRetry = vm::load,
        onAdd = onAdd,
        onOpen = onEdit,
    )
}

/** Every plan, inactive ones included, in display order. */
@Composable
fun PlansAdminScreen(onBack: () -> Unit, onAdd: () -> Unit, onEdit: (Long) -> Unit) {
    val container = appContainer()
    val vm: AdminListViewModel<SubscriptionPlanDto> = viewModel {
        AdminListViewModel({ container.owner.plans() }, container.admin.changes)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    AdminList(
        title = stringResource(R.string.manage_plans),
        state = state,
        rows = state.items.map {
            AdminRow(
                id = it.id,
                title = it.name,
                subtitle = "${egp(it.priceCents)} · ${stringResource(R.string.plan_duration, it.durationDays)}",
                active = it.active,
            )
        },
        onBack = onBack,
        onRetry = vm::load,
        onAdd = onAdd,
        onOpen = onEdit,
    )
}

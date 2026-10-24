package com.aghour.app.ui.firms

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aghour.app.R
import com.aghour.app.data.remote.ApiResult
import com.aghour.app.data.remote.FirmDto
import com.aghour.app.data.repository.CatalogRepository
import com.aghour.app.ui.components.AppTopBar
import com.aghour.app.ui.components.LoadingBox
import com.aghour.app.ui.components.MessageBox
import com.aghour.app.ui.components.Pill
import com.aghour.app.ui.components.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FirmDetailUiState(val loading: Boolean = true, val firm: FirmDto? = null, val error: String? = null)

class FirmDetailViewModel(private val catalog: CatalogRepository, private val firmId: Long) : ViewModel() {
    private val _state = MutableStateFlow(FirmDetailUiState())
    val state: StateFlow<FirmDetailUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = FirmDetailUiState(loading = true)
        viewModelScope.launch {
            _state.value = when (val result = catalog.firm(firmId)) {
                is ApiResult.Success -> FirmDetailUiState(loading = false, firm = result.data)
                is ApiResult.Failure -> FirmDetailUiState(loading = false, error = result.message)
            }
        }
    }
}

@Composable
fun FirmDetailScreen(firmId: Long, onBack: () -> Unit) {
    val container = appContainer()
    val vm: FirmDetailViewModel = viewModel { FirmDetailViewModel(container.catalog, firmId) }
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(topBar = { AppTopBar(state.firm?.name ?: "", onBack) }) { padding ->
        val firm = state.firm
        when {
            state.loading -> LoadingBox(Modifier.padding(padding))
            firm == null -> MessageBox(state.error ?: "", Modifier.padding(padding), onRetry = vm::load)
            else -> FirmDetailContent(firm, Modifier.padding(padding))
        }
    }
}

@Composable
private fun FirmDetailContent(firm: FirmDto, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Pill(firm.category.name, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(Modifier.height(8.dp))
                Text(firm.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                firm.description?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Quick actions
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            firm.phone?.let { phone ->
                Button(onClick = { context.openDialer(phone) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Call, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.call))
                }
            }
            firm.whatsapp?.let { number ->
                Button(
                    onClick = { context.openWhatsApp(number) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.whatsapp))
                }
            }
            if (firm.latitude != null && firm.longitude != null) {
                OutlinedButton(onClick = { context.openMap(firm.latitude, firm.longitude, firm.name) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.LocationOn, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.open_map))
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)) {
                DetailRow(stringResource(R.string.address), firm.address)
                HorizontalDivider()
                DetailRow(stringResource(R.string.working_hours), firm.workingHours)
                HorizontalDivider()
                DetailRow(stringResource(R.string.phone), firm.phone)
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String?) {
    Column(Modifier.padding(vertical = 10.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value?.takeIf { it.isNotBlank() } ?: "—", style = MaterialTheme.typography.bodyLarge)
    }
}

// ---- Intents ----

private fun Context.startSafely(intent: Intent) {
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // No app can handle it (e.g. no maps app); nothing to do.
    }
}

private fun Context.openDialer(phone: String) =
    startSafely(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))

/** Egyptian local numbers (01xxxxxxxxx) become international (201xxxxxxxxx) for wa.me links. */
private fun Context.openWhatsApp(number: String) {
    val digits = number.filter { it.isDigit() }
    val international = if (digits.startsWith("0")) "2$digits" else digits
    startSafely(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$international")))
}

private fun Context.openMap(lat: Double, lng: Double, label: String) =
    startSafely(Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})")))

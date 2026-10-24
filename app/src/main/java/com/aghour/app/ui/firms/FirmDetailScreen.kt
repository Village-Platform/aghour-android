package com.aghour.app.ui.firms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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
import com.aghour.app.ui.components.BrandHeader
import com.aghour.app.ui.components.LetterAvatar
import com.aghour.app.ui.components.MessageBox
import com.aghour.app.ui.components.Pill
import com.aghour.app.ui.components.SkeletonCards
import com.aghour.app.ui.components.appContainer
import com.aghour.app.ui.components.brandCardColors
import com.aghour.app.ui.components.brandCardElevation
import com.aghour.app.ui.components.openDialer
import com.aghour.app.ui.components.openMap
import com.aghour.app.ui.components.openWhatsApp
import com.aghour.app.ui.components.shareText
import com.aghour.app.ui.theme.Cream
import com.aghour.app.ui.theme.Gold100
import com.aghour.app.ui.theme.Gold500
import com.aghour.app.ui.theme.Green50
import com.aghour.app.ui.theme.Green900
import com.aghour.app.ui.theme.Green950
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
    val firm = state.firm

    when {
        firm != null -> FirmDetailContent(firm, onBack)
        state.loading -> Column(Modifier.fillMaxSize()) {
            AppTopBar(title = "", onBack = onBack)
            SkeletonCards(count = 3, modifier = Modifier.padding(top = 16.dp))
        }
        else -> Column(Modifier.fillMaxSize()) {
            AppTopBar(title = "", onBack = onBack)
            MessageBox(state.error ?: "", onRetry = vm::load)
        }
    }
}

@Composable
private fun FirmDetailContent(firm: FirmDto, onBack: () -> Unit) {
    val context = LocalContext.current
    val shareMessage = stringResource(R.string.share_firm_text, firm.name, firm.category.name, firm.phone ?: "")

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            // Hero header
            BrandHeader {
                Column(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(bottom = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = Cream)
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { context.shareText(shareMessage) }) {
                            Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.share), tint = Cream)
                        }
                    }
                    LetterAvatar(firm.name, size = 88.dp, background = Gold500, content = Green950, bordered = true)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        firm.name,
                        color = Cream,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Pill(firm.category.name, Gold100, Green900)
                }
            }

            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                firm.description?.takeIf { it.isNotBlank() }?.let { description ->
                    InfoCard(stringResource(R.string.about)) {
                        Text(description, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                InfoCard(stringResource(R.string.contact_info)) {
                    DetailRow(Icons.Filled.LocationOn, stringResource(R.string.address), firm.address)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    DetailRow(Icons.Filled.DateRange, stringResource(R.string.working_hours), firm.workingHours)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    DetailRow(Icons.Filled.Phone, stringResource(R.string.phone), firm.phone, ltr = true)
                }
            }
        }

        // Sticky action bar: the main reason people open a firm page is to contact it.
        ActionBar(firm)
    }
}

@Composable
private fun ActionBar(firm: FirmDto) {
    val context = LocalContext.current
    val hasMap = firm.latitude != null && firm.longitude != null
    if (firm.phone == null && firm.whatsapp == null && !hasMap) return

    Surface(shadowElevation = 12.dp, color = MaterialTheme.colorScheme.surface) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            firm.phone?.let { phone ->
                Button(onClick = { context.openDialer(phone) }, modifier = Modifier.weight(1f).height(50.dp)) {
                    Icon(Icons.Filled.Call, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.call))
                }
            }
            firm.whatsapp?.let { number ->
                Button(
                    onClick = { context.openWhatsApp(number) },
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold500, contentColor = Green950),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.whatsapp))
                }
            }
            if (hasMap) {
                OutlinedButton(
                    onClick = { context.openMap(firm.latitude!!, firm.longitude!!, firm.name) },
                    modifier = Modifier.weight(1f).height(50.dp),
                ) {
                    Icon(Icons.Filled.LocationOn, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.open_map))
                }
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable () -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), colors = brandCardColors(), elevation = brandCardElevation()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Info, null, Modifier.size(18.dp), tint = Gold500)
                Spacer(Modifier.width(6.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String?, ltr: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(38.dp).background(Green50, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Green900, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            // Phone numbers are read left-to-right even inside Arabic text.
            Text(
                (value?.takeIf { it.isNotBlank() } ?: "—").let { if (ltr) "\u2066$it\u2069" else it },
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

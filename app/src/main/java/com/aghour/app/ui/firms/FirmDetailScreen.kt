package com.aghour.app.ui.firms

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.aghour.app.ui.components.Badge
import com.aghour.app.ui.components.BadgeTone
import com.aghour.app.ui.components.MessageBox
import com.aghour.app.ui.components.OverlayIconButton
import com.aghour.app.ui.components.ScreenTitle
import com.aghour.app.ui.components.SheetLayout
import com.aghour.app.ui.components.SkeletonRows
import com.aghour.app.ui.components.SurfaceCard
import com.aghour.app.ui.components.appContainer
import com.aghour.app.ui.components.openDialer
import com.aghour.app.ui.components.openMap
import com.aghour.app.ui.components.openWhatsApp
import com.aghour.app.ui.components.shareText
import com.aghour.app.ui.theme.Aghour
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

    if (firm != null) {
        FirmDetailContent(firm, onBack)
    } else {
        Column(Modifier.fillMaxSize().background(Aghour.colors.page)) {
            ScreenTitle(title = "", onBack = onBack)
            if (state.loading) SkeletonRows(3, Modifier.padding(16.dp)) else MessageBox(state.error ?: "", onRetry = vm::load)
        }
    }
}

/** Design: village photo on top, a sheet with name, three action tiles and the details. */
@Composable
private fun FirmDetailContent(firm: FirmDto, onBack: () -> Unit) {
    val context = LocalContext.current
    val c = Aghour.colors
    val shareMessage = stringResource(R.string.share_firm_text, firm.name, firm.category.name, firm.phone ?: "")

    SheetLayout(
        sheetPadding = PaddingValues(20.dp),
        header = {
            Box(Modifier.fillMaxWidth().height(190.dp)) {
                Image(
                    painter = painterResource(R.drawable.brand_scene),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.30f)))
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    OverlayIconButton(onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = Color.White)
                    }
                    OverlayIconButton({ context.shareText(shareMessage) }) {
                        Icon(Icons.Filled.Share, stringResource(R.string.share), tint = Color.White)
                    }
                }
            }
        },
    ) {
        // Name + category
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(firm.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = c.ink)
                Text(
                    listOfNotNull(firm.category.name, firm.address?.takeIf { it.isNotBlank() }).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = c.muted,
                )
            }
            // Owners can open their own pending/suspended firms; say so clearly.
            when (firm.status) {
                "pending" -> Badge("قيد المراجعة", BadgeTone.Gold)
                "suspended" -> Badge("موقوف", BadgeTone.Red)
            }
        }

        // Call / location / WhatsApp tiles
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionTile(Icons.Filled.Call, stringResource(R.string.call), firm.phone != null, Modifier.weight(1f)) {
                firm.phone?.let { context.openDialer(it) }
            }
            ActionTile(
                Icons.Filled.LocationOn,
                stringResource(R.string.location),
                firm.latitude != null && firm.longitude != null,
                Modifier.weight(1f),
            ) {
                val lat = firm.latitude
                val lng = firm.longitude
                if (lat != null && lng != null) context.openMap(lat, lng, firm.name)
            }
            ActionTile(Icons.AutoMirrored.Filled.Send, stringResource(R.string.whatsapp), firm.whatsapp != null, Modifier.weight(1f)) {
                firm.whatsapp?.let { context.openWhatsApp(it) }
            }
        }

        // Hours + phone + address
        SurfaceCard {
            InfoLine(Icons.Outlined.Schedule, firm.workingHours)
            HorizontalDivider(color = c.line, modifier = Modifier.padding(vertical = 8.dp))
            // \u2066…\u2069 keeps the number left-to-right inside Arabic text.
            InfoLine(Icons.Outlined.PhoneAndroid, firm.phone?.let { "\u2066$it\u2069" })
            HorizontalDivider(color = c.line, modifier = Modifier.padding(vertical = 8.dp))
            InfoLine(Icons.Outlined.Place, firm.address)
        }

        firm.description?.takeIf { it.isNotBlank() }?.let { description ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.about_firm), style = MaterialTheme.typography.titleSmall, color = c.ink)
                SurfaceCard { Text(description, style = MaterialTheme.typography.bodyMedium, color = c.ink) }
            }
        }
    }
}

@Composable
private fun ActionTile(icon: ImageVector, label: String, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = Aghour.colors
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) c.goldInk else c.line, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = if (enabled) c.ink else c.muted)
    }
}

@Composable
private fun InfoLine(icon: ImageVector, value: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Aghour.colors.goldInk, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(value?.takeIf { it.isNotBlank() } ?: "—", style = MaterialTheme.typography.bodyMedium, color = Aghour.colors.ink)
    }
}

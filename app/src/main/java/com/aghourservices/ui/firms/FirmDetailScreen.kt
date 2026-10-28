package com.aghourservices.ui.firms

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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
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
import com.aghourservices.R
import com.aghourservices.data.remote.ApiResult
import com.aghourservices.data.remote.CategoryDto
import com.aghourservices.data.remote.FirmDto
import com.aghourservices.data.repository.CatalogRepository
import com.aghourservices.ui.components.Badge
import com.aghourservices.ui.components.BadgeTone
import com.aghourservices.ui.components.IconTile
import com.aghourservices.ui.components.MessageBox
import com.aghourservices.ui.components.OverlayIconButton
import com.aghourservices.ui.components.ScreenTitle
import com.aghourservices.ui.components.SheetLayout
import com.aghourservices.ui.components.SkeletonRows
import com.aghourservices.ui.components.SurfaceCard
import com.aghourservices.ui.components.appContainer
import com.aghourservices.ui.components.asHeading
import com.aghourservices.ui.components.categoryLine
import com.aghourservices.ui.components.firmIconUrl
import com.aghourservices.ui.components.openDialer
import com.aghourservices.ui.components.openMap
import com.aghourservices.ui.components.openWhatsApp
import com.aghourservices.ui.components.shareText
import com.aghourservices.ui.theme.Aghour
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FirmDetailUiState(
    val loading: Boolean = true,
    val firm: FirmDto? = null,
    val categories: List<CategoryDto> = emptyList(),
    val error: String? = null,
)

class FirmDetailViewModel(private val catalog: CatalogRepository, private val firmId: Long) : ViewModel() {
    private val _state = MutableStateFlow(FirmDetailUiState())
    val state: StateFlow<FirmDetailUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = FirmDetailUiState(loading = true)
        viewModelScope.launch {
            val categories = (catalog.categories() as? ApiResult.Success)?.data.orEmpty() // only for the icon
            _state.value = when (val result = catalog.firm(firmId)) {
                is ApiResult.Success -> FirmDetailUiState(loading = false, firm = result.data, categories = categories)
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
        FirmDetailContent(firm, firmIconUrl(firm, state.categories), onBack)
    } else {
        Column(Modifier.fillMaxSize().background(Aghour.colors.page)) {
            ScreenTitle(title = "", onBack = onBack)
            if (state.loading) SkeletonRows(3, Modifier.padding(16.dp)) else MessageBox(state.error ?: "", onRetry = vm::load)
        }
    }
}

/** Design: village photo on top, a sheet with name, three action tiles and the details. */
@Composable
private fun FirmDetailContent(firm: FirmDto, categoryIconUrl: String?, onBack: () -> Unit) {
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
        // Category icon + name + category
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(categoryIconUrl, size = 52.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(firm.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = c.ink, modifier = Modifier.asHeading())
                Text(
                    categoryLine(firm.category.name, firm.address),
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
            ActionTile(
                rememberVectorPainter(Icons.Filled.Call),
                stringResource(R.string.call),
                if (c.isDark) CallDark else CallLight,
                firm.phone != null,
                Modifier.weight(1f),
            ) {
                firm.phone?.let { context.openDialer(it) }
            }
            ActionTile(
                rememberVectorPainter(Icons.Filled.LocationOn),
                stringResource(R.string.location),
                if (c.isDark) MapDark else MapLight,
                firm.latitude != null && firm.longitude != null,
                Modifier.weight(1f),
            ) {
                val lat = firm.latitude
                val lng = firm.longitude
                if (lat != null && lng != null) context.openMap(lat, lng, firm.name)
            }
            ActionTile(
                painterResource(R.drawable.ic_whatsapp),
                stringResource(R.string.whatsapp),
                WhatsAppGreen,
                firm.whatsapp != null,
                Modifier.weight(1f),
            ) {
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
                Text(stringResource(R.string.about_firm), style = MaterialTheme.typography.titleSmall, color = c.ink, modifier = Modifier.asHeading())
                SurfaceCard { Text(description, style = MaterialTheme.typography.bodyMedium, color = c.ink) }
            }
        }
    }
}

// Action colours (light / dark shade): green call, red map pin, WhatsApp brand green.
private val CallLight = Color(0xFF1A7F37)
private val CallDark = Color(0xFF3FB950)
private val MapLight = Color(0xFFD1242F)
private val MapDark = Color(0xFFFF7B72)
private val WhatsAppGreen = Color(0xFF25D366)

/** Call / location / WhatsApp tile: the icon in its own colour on a light tint of it; grey when unavailable. */
@Composable
private fun ActionTile(icon: Painter, label: String, color: Color, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = Aghour.colors
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier
            .clip(shape)
            .background(if (enabled) color.copy(alpha = if (c.isDark) 0.16f else 0.10f) else c.surface)
            .border(1.dp, if (enabled) color.copy(alpha = 0.35f) else c.line, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) color else c.line, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (enabled) c.ink else c.muted)
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

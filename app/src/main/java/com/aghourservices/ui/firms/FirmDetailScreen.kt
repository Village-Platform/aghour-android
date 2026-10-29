package com.aghourservices.ui.firms

import android.widget.Toast
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.aghourservices.data.repository.AdminRepository
import com.aghourservices.data.repository.CatalogRepository
import com.aghourservices.ui.components.Badge
import com.aghourservices.ui.components.BadgeTone
import com.aghourservices.ui.components.ErrorBanner
import com.aghourservices.ui.components.GhostButton
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
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FirmDetailUiState(
    val loading: Boolean = true,
    val firm: FirmDto? = null,
    val categories: List<CategoryDto> = emptyList(),
    val error: String? = null,
    /** Super admin actions: a status change or delete is running / failed / finished. */
    val busy: Boolean = false,
    val actionError: String? = null,
    val deleted: Boolean = false,
)

class FirmDetailViewModel(
    private val catalog: CatalogRepository,
    private val admin: AdminRepository,
    private val firmId: Long,
) : ViewModel() {
    private val _state = MutableStateFlow(FirmDetailUiState())
    val state: StateFlow<FirmDetailUiState> = _state.asStateFlow()

    init {
        load()
        // Edited elsewhere (the form) -> show the new data. Not while deleting: the firm is gone.
        viewModelScope.launch {
            admin.firmsVersion.drop(1).collect { if (!_state.value.busy && !_state.value.deleted) load() }
        }
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

    fun setStatus(status: String) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, actionError = null) }
        viewModelScope.launch {
            when (val r = admin.setFirmStatus(firmId, status)) {
                is ApiResult.Success -> _state.update { it.copy(busy = false, firm = r.data) }
                is ApiResult.Failure -> _state.update { it.copy(busy = false, actionError = r.message) }
            }
        }
    }

    fun delete() {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, actionError = null) }
        viewModelScope.launch {
            when (val r = admin.deleteFirm(firmId)) {
                is ApiResult.Success -> _state.update { it.copy(busy = false, deleted = true) }
                is ApiResult.Failure -> _state.update { it.copy(busy = false, actionError = r.message) }
            }
        }
    }
}

/**
 * [onEdit] is null for everyone but the super admin (decided in AppNavigation); null hides the
 * edit / approve / suspend / delete controls.
 */
@Composable
fun FirmDetailScreen(firmId: Long, onBack: () -> Unit, onEdit: (() -> Unit)? = null, onDeleted: () -> Unit = onBack) {
    val container = appContainer()
    val vm: FirmDetailViewModel = viewModel { FirmDetailViewModel(container.catalog, container.admin, firmId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val firm = state.firm
    val context = LocalContext.current
    val deletedMessage = stringResource(R.string.firm_deleted)

    LaunchedEffect(state.deleted) {
        if (state.deleted) {
            Toast.makeText(context, deletedMessage, Toast.LENGTH_SHORT).show()
            onDeleted()
        }
    }

    if (firm != null) {
        FirmDetailContent(firm, firmIconUrl(firm, state.categories), onBack) {
            if (onEdit != null) {
                AdminFirmControls(
                    firm = firm,
                    busy = state.busy,
                    error = state.actionError,
                    onEdit = onEdit,
                    onSetStatus = vm::setStatus,
                    onDelete = vm::delete,
                )
            }
        }
    } else {
        Column(Modifier.fillMaxSize().background(Aghour.colors.page)) {
            ScreenTitle(title = "", onBack = onBack)
            if (state.loading) SkeletonRows(3, Modifier.padding(16.dp)) else MessageBox(state.error ?: "", onRetry = vm::load)
        }
    }
}

/** Super admin: edit, approve or suspend, and delete (after a confirmation). */
@Composable
private fun AdminFirmControls(
    firm: FirmDto,
    busy: Boolean,
    error: String?,
    onEdit: () -> Unit,
    onSetStatus: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val c = Aghour.colors
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.admin_section), style = MaterialTheme.typography.titleSmall, color = c.ink, modifier = Modifier.asHeading())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GhostButton(stringResource(R.string.edit), onEdit, Modifier.weight(1f))
            if (firm.status == "active") {
                GhostButton(stringResource(R.string.suspend), { if (!busy) onSetStatus("suspended") }, Modifier.weight(1f))
            } else {
                GhostButton(stringResource(R.string.approve), { if (!busy) onSetStatus("active") }, Modifier.weight(1f))
            }
            Button(
                onClick = { if (!busy) confirmDelete = true },
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = c.bad.copy(alpha = 0.10f), contentColor = c.bad),
            ) { Text(stringResource(R.string.delete), style = MaterialTheme.typography.labelLarge) }
        }
        ErrorBanner(error)
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_firm_title)) },
            text = { Text(stringResource(R.string.delete_firm_body, firm.name)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text(stringResource(R.string.delete), color = c.bad) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel), color = c.ink) } },
        )
    }
}

/** Design: village photo on top, a sheet with name, three action tiles and the details. */
@Composable
private fun FirmDetailContent(
    firm: FirmDto,
    categoryIconUrl: String?,
    onBack: () -> Unit,
    footer: @Composable () -> Unit = {},
) {
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

        footer()
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

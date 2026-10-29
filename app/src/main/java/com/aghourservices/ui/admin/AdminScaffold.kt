package com.aghourservices.ui.admin

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aghourservices.R
import com.aghourservices.data.remote.ApiResult
import com.aghourservices.ui.components.Badge
import com.aghourservices.ui.components.BadgeTone
import com.aghourservices.ui.components.Chevron
import com.aghourservices.ui.components.ConfirmDeleteDialog
import com.aghourservices.ui.components.DangerButton
import com.aghourservices.ui.components.ErrorBanner
import com.aghourservices.ui.components.GoldButton
import com.aghourservices.ui.components.IconTile
import com.aghourservices.ui.components.MessageBox
import com.aghourservices.ui.components.ScreenTitle
import com.aghourservices.ui.components.SkeletonRows
import com.aghourservices.ui.components.SurfaceCard
import com.aghourservices.ui.theme.Aghour
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

// ---------------------------------------------------------------- forms

/** Shown in a confirmation dialog before [onDelete] runs. */
data class DeleteAction(val title: String, val body: String, val onDelete: () -> Unit)

/**
 * Frame shared by the admin forms: title, loading / load error, the [fields], the error banner,
 * save and, when [delete] is set (editing), a delete button with confirmation.
 */
@Composable
fun AdminFormScaffold(
    title: String,
    onBack: () -> Unit,
    loading: Boolean,
    loadError: String?,
    onRetry: () -> Unit,
    error: String?,
    submitting: Boolean,
    onSave: () -> Unit,
    delete: DeleteAction? = null,
    fields: @Composable ColumnScope.() -> Unit,
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Aghour.colors.page).imePadding()) {
        ScreenTitle(title, onBack = onBack)
        when {
            loading -> SkeletonRows(4, Modifier.padding(16.dp))
            loadError != null -> MessageBox(loadError, onRetry = onRetry)
            else -> Column(
                Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                fields()
                ErrorBanner(error)
                GoldButton(stringResource(R.string.save), onSave, loading = submitting)
                if (delete != null) DangerButton(stringResource(R.string.delete), { if (!submitting) confirmDelete = true })
            }
        }
    }

    if (confirmDelete && delete != null) {
        ConfirmDeleteDialog(
            title = delete.title,
            body = delete.body,
            onConfirm = {
                confirmDelete = false
                delete.onDelete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

/** Small grey label above a non-text field (chips, segmented control, image). */
@Composable
fun FieldLabel(text: String) = Text(text, style = MaterialTheme.typography.labelMedium, color = Aghour.colors.muted)

@Composable
fun FieldError(error: Int?) {
    if (error != null) Text(stringResource(error), color = Aghour.colors.bad, style = MaterialTheme.typography.bodySmall)
}

// ---------------------------------------------------------------- lists

data class AdminListUiState<T>(val loading: Boolean = true, val items: List<T> = emptyList(), val error: String? = null)

/** Loads a list for an admin screen, and again whenever a super admin write succeeds. */
class AdminListViewModel<T>(
    private val fetch: suspend () -> ApiResult<List<T>>,
    changes: StateFlow<Int>,
) : ViewModel() {
    private val _state = MutableStateFlow(AdminListUiState<T>())
    val state: StateFlow<AdminListUiState<T>> = _state.asStateFlow()

    init {
        load()
        viewModelScope.launch { changes.drop(1).collect { load() } }
    }

    fun load() {
        _state.update { it.copy(loading = it.items.isEmpty(), error = null) }
        viewModelScope.launch {
            when (val r = fetch()) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, items = r.data) }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = r.message) }
            }
        }
    }
}

/** One row of an admin list. [iconUrl] is drawn only when [showIcon] (categories). */
data class AdminRow(
    val id: Long,
    val title: String,
    val subtitle: String?,
    val active: Boolean = true,
    val showIcon: Boolean = false,
    val iconUrl: String? = null,
)

@Composable
fun AdminList(
    title: String,
    state: AdminListUiState<*>,
    rows: List<AdminRow>,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
) {
    val c = Aghour.colors
    Column(Modifier.fillMaxSize().background(c.page)) {
        ScreenTitle(title, onBack = onBack) {
            IconButton(onClick = onAdd) { Icon(Icons.Filled.Add, stringResource(R.string.add), tint = c.goldInk) }
        }
        when {
            state.loading -> SkeletonRows(4, Modifier.padding(16.dp))
            state.error != null -> MessageBox(state.error, onRetry = onRetry)
            rows.isEmpty() -> MessageBox(stringResource(R.string.admin_list_empty))
            else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(rows, key = { it.id }) { row ->
                    SurfaceCard(onClick = { onOpen(row.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (row.showIcon) {
                                IconTile(row.iconUrl, size = 40.dp)
                                Spacer(Modifier.width(12.dp))
                            }
                            Column(Modifier.weight(1f)) {
                                Text(row.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = c.ink)
                                row.subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = c.muted) }
                            }
                            if (!row.active) {
                                Badge(stringResource(R.string.inactive), BadgeTone.Gray)
                                Spacer(Modifier.width(8.dp))
                            }
                            Chevron()
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- category icon

private const val ICON_MAX_PX = 256

/**
 * Reads a picked image and re-encodes it as a PNG of at most 256px. That always stays under the
 * server's 512KB limit (256 × 256 × 4 bytes ≈ 262KB before compression) and is a format it accepts.
 * Big photos are decoded with inSampleSize, so they never load at full size. null = unreadable.
 */
suspend fun loadIconPng(context: Context, uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
    runCatching {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val largest = max(bounds.outWidth, bounds.outHeight)
        if (largest <= 0) return@runCatching null

        var sample = 1
        while (largest / (sample * 2) >= ICON_MAX_PX) sample *= 2
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return@runCatching null

        val scale = ICON_MAX_PX.toFloat() / max(decoded.width, decoded.height)
        val icon = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).roundToInt().coerceAtLeast(1),
                (decoded.height * scale).roundToInt().coerceAtLeast(1),
                true,
            )
        } else {
            decoded
        }
        ByteArrayOutputStream().use { out ->
            icon.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.toByteArray()
        }
    }.getOrNull()
}

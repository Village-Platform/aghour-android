package com.aghourservices.ui.admin

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aghourservices.R
import com.aghourservices.data.remote.ApiResult
import com.aghourservices.data.repository.AdminRepository
import com.aghourservices.data.repository.CategoryInput
import com.aghourservices.ui.components.FormField
import com.aghourservices.ui.components.GhostButton
import com.aghourservices.ui.components.IconTile
import com.aghourservices.ui.components.SwitchRow
import com.aghourservices.ui.components.appContainer
import com.aghourservices.ui.components.categoryIconUrl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoryFormUiState(
    val loading: Boolean = true,
    val loadError: String? = null,
    val name: String = "",
    val description: String = "",
    val position: String = "0",
    val active: Boolean = true,
    /** Current image on the server (relative URL). */
    val iconUrl: String? = null,
    /** A newly picked image, already scaled down; uploaded on save. */
    val newIcon: ByteArray? = null,
    val nameError: Int? = null,
    val positionError: Int? = null,
    val imageError: Int? = null,
    val submitting: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
    val deleted: Boolean = false,
)

class CategoryFormViewModel(private val admin: AdminRepository, private val categoryId: Long?) : ViewModel() {
    private val _state = MutableStateFlow(CategoryFormUiState(loading = categoryId != null))
    val state: StateFlow<CategoryFormUiState> = _state.asStateFlow()

    init {
        if (categoryId != null) load()
    }

    fun load() {
        val id = categoryId ?: return
        _state.update { it.copy(loading = true, loadError = null) }
        viewModelScope.launch {
            when (val r = admin.category(id)) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        loading = false,
                        name = r.data.name,
                        description = r.data.description.orEmpty(),
                        position = r.data.position.toString(),
                        active = r.data.active,
                        iconUrl = r.data.iconUrl,
                    )
                }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, loadError = r.message) }
            }
        }
    }

    fun update(transform: CategoryFormUiState.() -> CategoryFormUiState) =
        _state.update { it.transform().copy(error = null, nameError = null, positionError = null, imageError = null) }

    /** null = the picked file could not be read as an image. */
    fun onIconPicked(png: ByteArray?) =
        if (png == null) _state.update { it.copy(imageError = R.string.error_image_unreadable) } else update { copy(newIcon = png) }

    fun submit() {
        val s = _state.value
        if (s.submitting) return
        val position = s.position.trim().toIntOrNull()
        val nameError = if (s.name.isBlank()) R.string.error_required else null
        val positionError = if (position == null || position < 0) R.string.error_whole_number else null
        if (nameError != null || positionError != null || position == null) {
            _state.update { it.copy(nameError = nameError, positionError = positionError) }
            return
        }
        val input = CategoryInput(s.name, s.description, position, s.active, s.newIcon)
        _state.update { it.copy(submitting = true) }
        viewModelScope.launch {
            val result = if (categoryId == null) admin.createCategory(input) else admin.updateCategory(categoryId, input)
            when (result) {
                is ApiResult.Success -> _state.update { it.copy(submitting = false, saved = true) }
                is ApiResult.Failure -> _state.update { it.copy(submitting = false, error = result.message) }
            }
        }
    }

    fun delete() {
        val id = categoryId ?: return
        if (_state.value.submitting) return
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            when (val r = admin.deleteCategory(id)) {
                is ApiResult.Success -> _state.update { it.copy(submitting = false, deleted = true) }
                is ApiResult.Failure -> _state.update { it.copy(submitting = false, error = r.message) }
            }
        }
    }
}

/** Create ([categoryId] null) or edit a category, including its image. Closes itself when done. */
@Composable
fun CategoryFormScreen(categoryId: Long?, onBack: () -> Unit, onDone: () -> Unit) {
    val container = appContainer()
    val vm: CategoryFormViewModel = viewModel { CategoryFormViewModel(container.admin, categoryId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savedMessage = stringResource(R.string.saved)
    val deletedMessage = stringResource(R.string.deleted)

    LaunchedEffect(state.saved, state.deleted) {
        if (state.saved || state.deleted) {
            Toast.makeText(context, if (state.deleted) deletedMessage else savedMessage, Toast.LENGTH_SHORT).show()
            onDone()
        }
    }

    // System photo picker: no storage permission needed.
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { vm.onIconPicked(loadIconPng(context, uri)) }
    }

    AdminFormScaffold(
        title = stringResource(if (categoryId == null) R.string.add_category else R.string.edit_category),
        onBack = onBack,
        loading = state.loading,
        loadError = state.loadError,
        onRetry = vm::load,
        error = state.error,
        submitting = state.submitting,
        onSave = vm::submit,
        delete = categoryId?.let {
            DeleteAction(
                title = stringResource(R.string.delete_category_title),
                body = stringResource(R.string.delete_category_body, state.name),
                onDelete = vm::delete,
            )
        },
    ) {
        FieldLabel(stringResource(R.string.category_image))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val picked = state.newIcon
            if (picked != null) {
                val bitmap = remember(picked) { BitmapFactory.decodeByteArray(picked, 0, picked.size)?.asImageBitmap() }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)),
                    )
                }
            } else {
                IconTile(categoryIconUrl(state.iconUrl), size = 56.dp)
            }
            Spacer(Modifier.width(12.dp))
            GhostButton(
                stringResource(R.string.choose_image),
                { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                Modifier.weight(1f),
            )
        }
        FieldError(state.imageError)

        FormField(
            label = stringResource(R.string.category_name),
            value = state.name,
            onChange = { v -> vm.update { copy(name = v) } },
            error = state.nameError,
        )
        FormField(
            label = stringResource(R.string.position),
            value = state.position,
            onChange = { v -> vm.update { copy(position = v) } },
            error = state.positionError,
            keyboardType = KeyboardType.Number,
            ltr = true,
        )
        SwitchRow(stringResource(R.string.active), state.active, { v -> vm.update { copy(active = v) } })
        FormField(
            label = stringResource(R.string.description),
            value = state.description,
            onChange = { v -> vm.update { copy(description = v) } },
            imeAction = ImeAction.Default,
            multiline = true,
        )
    }
}

package com.aghourservices.ui.admin

import android.widget.Toast
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aghourservices.R
import com.aghourservices.data.remote.ApiResult
import com.aghourservices.data.repository.AdminRepository
import com.aghourservices.data.repository.ArticleInput
import com.aghourservices.data.repository.CatalogRepository
import com.aghourservices.ui.components.FormField
import com.aghourservices.ui.components.Segmented
import com.aghourservices.ui.components.SwitchRow
import com.aghourservices.ui.components.appContainer
import com.aghourservices.ui.components.kindLabel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.OffsetDateTime

/** Article kinds the server accepts (Article#kind). */
private val ArticleKinds = listOf("news", "announcement", "article")

data class ArticleFormUiState(
    val loading: Boolean = true,
    val loadError: String? = null,
    val title: String = "",
    val body: String = "",
    val kind: String = "news",
    val published: Boolean = true,
    /** Kept when an already published article is edited, so its date does not move. */
    val publishedAt: String? = null,
    val titleError: Int? = null,
    val bodyError: Int? = null,
    val submitting: Boolean = false,
    val error: String? = null,
    val savedId: Long? = null,
    val deleted: Boolean = false,
)

class ArticleFormViewModel(
    private val catalog: CatalogRepository,
    private val admin: AdminRepository,
    private val articleId: Long?,
) : ViewModel() {
    private val _state = MutableStateFlow(ArticleFormUiState(loading = articleId != null))
    val state: StateFlow<ArticleFormUiState> = _state.asStateFlow()

    init {
        if (articleId != null) load()
    }

    fun load() {
        val id = articleId ?: return
        _state.update { it.copy(loading = true, loadError = null) }
        viewModelScope.launch {
            when (val r = catalog.article(id)) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        loading = false,
                        title = r.data.title,
                        body = r.data.body,
                        kind = r.data.kind,
                        published = r.data.publishedAt != null,
                        publishedAt = r.data.publishedAt,
                    )
                }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, loadError = r.message) }
            }
        }
    }

    fun update(transform: ArticleFormUiState.() -> ArticleFormUiState) =
        _state.update { it.transform().copy(error = null, titleError = null, bodyError = null) }

    fun submit() {
        val s = _state.value
        if (s.submitting) return
        val titleError = if (s.title.isBlank()) R.string.error_required else null
        val bodyError = if (s.body.isBlank()) R.string.error_required else null
        if (titleError != null || bodyError != null) {
            _state.update { it.copy(titleError = titleError, bodyError = bodyError) }
            return
        }
        // Published: keep the original date, or publish now. Draft: no date.
        val publishedAt = if (s.published) s.publishedAt ?: OffsetDateTime.now().toString() else null
        val input = ArticleInput(s.title, s.body, s.kind, publishedAt)
        _state.update { it.copy(submitting = true) }
        viewModelScope.launch {
            val result = if (articleId == null) admin.createArticle(input) else admin.updateArticle(articleId, input)
            when (result) {
                is ApiResult.Success -> _state.update { it.copy(submitting = false, savedId = result.data.id) }
                is ApiResult.Failure -> _state.update { it.copy(submitting = false, error = result.message) }
            }
        }
    }

    fun delete() {
        val id = articleId ?: return
        if (_state.value.submitting) return
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            when (val r = admin.deleteArticle(id)) {
                is ApiResult.Success -> _state.update { it.copy(submitting = false, deleted = true) }
                is ApiResult.Failure -> _state.update { it.copy(submitting = false, error = r.message) }
            }
        }
    }
}

/** Create ([articleId] null) or edit a news item / announcement / article. */
@Composable
fun ArticleFormScreen(articleId: Long?, onBack: () -> Unit, onSaved: (Long) -> Unit, onDeleted: () -> Unit) {
    val container = appContainer()
    val vm: ArticleFormViewModel = viewModel { ArticleFormViewModel(container.catalog, container.admin, articleId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val savedMessage = stringResource(R.string.saved)
    val deletedMessage = stringResource(R.string.deleted)

    LaunchedEffect(state.savedId) {
        state.savedId?.let {
            Toast.makeText(context, savedMessage, Toast.LENGTH_SHORT).show()
            onSaved(it)
        }
    }
    LaunchedEffect(state.deleted) {
        if (state.deleted) {
            Toast.makeText(context, deletedMessage, Toast.LENGTH_SHORT).show()
            onDeleted()
        }
    }

    AdminFormScaffold(
        title = stringResource(if (articleId == null) R.string.add_article else R.string.edit_article),
        onBack = onBack,
        loading = state.loading,
        loadError = state.loadError,
        onRetry = vm::load,
        error = state.error,
        submitting = state.submitting,
        onSave = vm::submit,
        delete = articleId?.let {
            DeleteAction(
                title = stringResource(R.string.delete_article_title),
                body = stringResource(R.string.delete_article_body, state.title),
                onDelete = vm::delete,
            )
        },
    ) {
        FormField(
            label = stringResource(R.string.article_title),
            value = state.title,
            onChange = { v -> vm.update { copy(title = v) } },
            error = state.titleError,
        )
        FieldLabel(stringResource(R.string.article_kind))
        Row {
            Segmented(
                options = ArticleKinds.map { it to kindLabel(it) },
                selected = state.kind,
                onSelect = { k -> vm.update { copy(kind = k) } },
            )
        }
        SwitchRow(stringResource(R.string.published), state.published, { v -> vm.update { copy(published = v) } })
        FormField(
            label = stringResource(R.string.article_body),
            value = state.body,
            onChange = { v -> vm.update { copy(body = v) } },
            error = state.bodyError,
            imeAction = ImeAction.Default,
            multiline = true,
        )
    }
}

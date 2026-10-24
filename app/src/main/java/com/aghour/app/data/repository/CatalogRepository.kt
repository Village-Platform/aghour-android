package com.aghour.app.data.repository

import com.aghour.app.data.remote.AghourApi
import com.aghour.app.data.remote.ApiResult
import com.aghour.app.data.remote.ArticleDto
import com.aghour.app.data.remote.CategoryDto
import com.aghour.app.data.remote.FirmDto
import com.aghour.app.data.remote.ListResponse
import com.aghour.app.data.remote.safeApiCall
import kotlinx.serialization.json.Json

/** Public browsing data: categories, firms and news. */
class CatalogRepository(private val api: AghourApi, private val json: Json) {

    suspend fun categories(): ApiResult<List<CategoryDto>> =
        safeApiCall(json) { api.categories().data }

    suspend fun categoryFirms(categoryId: Long, query: String?, page: Int): ApiResult<ListResponse<FirmDto>> =
        safeApiCall(json) { api.categoryFirms(categoryId, query?.takeIf { it.isNotBlank() }, page) }

    /** Explore: all visible firms, filtered by name and/or category. */
    suspend fun firms(query: String?, categoryId: Long?, page: Int, perPage: Int = 20): ApiResult<ListResponse<FirmDto>> =
        safeApiCall(json) {
            api.firms(query = query?.trim()?.takeIf { it.isNotEmpty() }, categoryId = categoryId, page = page, perPage = perPage)
        }

    suspend fun firm(id: Long): ApiResult<FirmDto> =
        safeApiCall(json) { api.firm(id).data }

    suspend fun articles(page: Int, perPage: Int = 20, kind: String? = null): ApiResult<ListResponse<ArticleDto>> =
        safeApiCall(json) { api.articles(kind = kind, page = page, perPage = perPage) }

    suspend fun article(id: Long): ApiResult<ArticleDto> =
        safeApiCall(json) { api.article(id).data }
}

package com.aghour.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// ---- Envelopes (match the Rails API: { data, meta } and { error }) ----

@Serializable
data class ListResponse<T>(val data: List<T>, val meta: PageMeta)

@Serializable
data class ItemResponse<T>(val data: T)

@Serializable
data class PageMeta(
    val page: Int,
    @SerialName("per_page") val perPage: Int,
    val total: Int,
    @SerialName("total_pages") val totalPages: Int,
)

@Serializable
data class ErrorEnvelope(val error: ApiErrorBody)

@Serializable
data class ApiErrorBody(
    val code: String,
    val message: String,
    val details: JsonObject? = null,
)

// ---- Resources ----

@Serializable
data class CategoryDto(
    val id: Long,
    val name: String,
    val description: String? = null,
    val icon: String? = null,
    val position: Int = 0,
)

@Serializable
data class CategoryRefDto(val id: Long, val name: String)

@Serializable
data class FirmDto(
    val id: Long,
    val name: String,
    val description: String? = null,
    val phone: String? = null,
    val whatsapp: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("working_hours") val workingHours: String? = null,
    val status: String,
    val category: CategoryRefDto,
)

@Serializable
data class AuthorDto(val id: Long, val name: String)

@Serializable
data class ArticleDto(
    val id: Long,
    val title: String,
    val body: String,
    val kind: String,
    @SerialName("published_at") val publishedAt: String? = null,
    val author: AuthorDto,
)

@Serializable
data class UserDto(
    val id: Long,
    val name: String,
    val phone: String,
    val email: String? = null,
    val role: String,
)

@Serializable
data class AuthPayload(val token: String, val user: UserDto)

// ---- Requests ----

@Serializable
data class LoginRequest(val phone: String, val password: String)

@Serializable
data class RegisterRequest(val user: RegisterUser)

@Serializable
data class RegisterUser(
    val name: String,
    val phone: String,
    val email: String? = null,
    val password: String,
    @SerialName("password_confirmation") val passwordConfirmation: String,
)

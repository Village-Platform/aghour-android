package com.aghourservices.data.repository

import com.aghourservices.data.remote.AghourApi
import com.aghourservices.data.remote.ApiResult
import com.aghourservices.data.remote.FirmDto
import com.aghourservices.data.remote.safeApiCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import retrofit2.HttpException
import retrofit2.Response

/** Firm fields a super admin can set. Blank text is sent as null so a field can be cleared. */
data class FirmInput(
    val categoryId: Long,
    val name: String,
    val description: String,
    val phone: String,
    val whatsapp: String,
    val address: String,
    val latitude: Double?,
    val longitude: Double?,
    val workingHours: String,
    val status: String,
)

/**
 * Super admin writes. The server's policies decide what is allowed; this class only sends.
 * [firmsVersion] goes up after every successful firm write so screens showing firms can reload.
 */
class AdminRepository(private val api: AghourApi, private val json: Json) {

    private val _firmsVersion = MutableStateFlow(0)
    val firmsVersion: StateFlow<Int> = _firmsVersion.asStateFlow()

    suspend fun createFirm(input: FirmInput): ApiResult<FirmDto> =
        safeApiCall(json) { api.createFirm(input.toBody()).data }.alsoOnSuccess(::firmsChanged)

    suspend fun updateFirm(id: Long, input: FirmInput): ApiResult<FirmDto> =
        safeApiCall(json) { api.updateFirm(id, input.toBody()).data }.alsoOnSuccess(::firmsChanged)

    /** Approve (active) or suspend without opening the form. */
    suspend fun setFirmStatus(id: Long, status: String): ApiResult<FirmDto> =
        safeApiCall(json) {
            api.updateFirm(id, buildJsonObject { put("firm", buildJsonObject { put("status", status) }) }).data
        }.alsoOnSuccess(::firmsChanged)

    suspend fun deleteFirm(id: Long): ApiResult<Unit> =
        safeApiCall(json) { api.deleteFirm(id).orThrow() }.alsoOnSuccess(::firmsChanged)

    private fun firmsChanged() = _firmsVersion.update { it + 1 }
}

private fun FirmInput.toBody(): JsonObject = buildJsonObject {
    put(
        "firm",
        buildJsonObject {
            put("category_id", categoryId)
            put("name", name.trim())
            put("description", description.trim().ifEmpty { null })
            put("phone", phone.trim().ifEmpty { null })
            put("whatsapp", whatsapp.trim().ifEmpty { null })
            put("address", address.trim().ifEmpty { null })
            put("latitude", latitude)
            put("longitude", longitude)
            put("working_hours", workingHours.trim().ifEmpty { null })
            put("status", status)
        },
    )
}

/** Turns a non-2xx Response into the HttpException that safeApiCall knows how to read. */
private fun Response<Unit>.orThrow() {
    if (!isSuccessful) throw HttpException(this)
}

private inline fun <T> ApiResult<T>.alsoOnSuccess(block: () -> Unit): ApiResult<T> =
    also { if (it is ApiResult.Success) block() }

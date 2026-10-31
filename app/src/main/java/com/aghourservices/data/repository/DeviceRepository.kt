package com.aghourservices.data.repository

import com.aghourservices.BuildConfig
import com.aghourservices.data.remote.AghourApi
import com.aghourservices.data.remote.ApiResult
import com.aghourservices.data.remote.DeviceDto
import com.aghourservices.data.remote.RegisterDeviceRequest
import com.aghourservices.data.remote.TopicDto
import com.aghourservices.data.remote.safeApiCall
import com.aghourservices.data.settings.SettingsStore
import kotlinx.serialization.json.Json

class DeviceRepository(
    private val api: AghourApi,
    private val settings: SettingsStore,
    private val json: Json,
) {
    suspend fun registerDevice(fcmToken: String): ApiResult<DeviceDto> {
        val installationId = settings.installationId.value ?: return ApiResult.Failure("No installation ID")
        val body = RegisterDeviceRequest(
            token = fcmToken,
            locale = "ar",
            appVersion = BuildConfig.VERSION_NAME,
            platform = "android",
        )
        return safeApiCall(json) { api.registerDevice(installationId, body).data }
    }

    suspend fun detachDevice(): ApiResult<DeviceDto?> {
        val installationId = settings.installationId.value ?: return ApiResult.Success(null)
        return when (val result = safeApiCall(json) { api.detachDevice(installationId).data }) {
            is ApiResult.Success -> ApiResult.Success(result.data)
            is ApiResult.Failure -> result
        }
    }

    suspend fun topics(): ApiResult<List<TopicDto>> =
        safeApiCall(json) { api.topics().data }

    suspend fun subscribeTopic(key: String): ApiResult<TopicDto> {
        val installationId = settings.installationId.value ?: return ApiResult.Failure("No installation ID")
        return safeApiCall(json) { api.subscribeTopic(installationId, key).data }
    }

    suspend fun unsubscribeTopic(key: String): ApiResult<TopicDto> {
        val installationId = settings.installationId.value ?: return ApiResult.Failure("No installation ID")
        return safeApiCall(json) { api.unsubscribeTopic(installationId, key).data }
    }
}

package com.aghourservices.data.repository

import com.aghourservices.data.remote.AghourApi
import com.aghourservices.data.remote.ApiResult
import com.aghourservices.data.remote.LoginRequest
import com.aghourservices.data.remote.RegisterRequest
import com.aghourservices.data.remote.RegisterUser
import com.aghourservices.data.remote.UpdateMeRequest
import com.aghourservices.data.remote.UpdateMeUser
import com.aghourservices.data.remote.UserDto
import com.aghourservices.data.remote.safeApiCall
import com.aghourservices.data.session.SessionStore
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json

class AuthRepository(
    private val api: AghourApi,
    private val session: SessionStore,
    private val json: Json,
) {
    val currentUser: StateFlow<UserDto?> = session.user

    suspend fun login(phone: String, password: String): ApiResult<UserDto> =
        when (val result = safeApiCall(json) { api.login(LoginRequest(phone.trim(), password)).data }) {
            is ApiResult.Success -> {
                session.save(result.data.token, result.data.user)
                ApiResult.Success(result.data.user)
            }
            is ApiResult.Failure -> result
        }

    suspend fun register(
        name: String,
        phone: String,
        email: String?,
        password: String,
        passwordConfirmation: String,
    ): ApiResult<UserDto> {
        val body = RegisterRequest(
            RegisterUser(
                name = name.trim(),
                phone = phone.trim(),
                email = email?.trim()?.takeIf { it.isNotEmpty() },
                password = password,
                passwordConfirmation = passwordConfirmation,
            ),
        )
        return when (val result = safeApiCall(json) { api.register(body).data }) {
            is ApiResult.Success -> {
                session.save(result.data.token, result.data.user)
                ApiResult.Success(result.data.user)
            }
            is ApiResult.Failure -> result
        }
    }

    /** Refreshes the stored user (role may change after a subscription is approved). */
    suspend fun refreshMe(): ApiResult<UserDto> =
        when (val result = safeApiCall(json) { api.me().data }) {
            is ApiResult.Success -> {
                session.updateUser(result.data)
                result
            }
            is ApiResult.Failure -> result
        }

    suspend fun changePassword(password: String, confirmation: String): ApiResult<UserDto> =
        safeApiCall(json) { api.updateMe(UpdateMeRequest(UpdateMeUser(password, confirmation))).data }

    suspend fun logout() = session.clear()
}

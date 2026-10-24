package com.aghour.app.data.repository

import com.aghour.app.data.remote.AghourApi
import com.aghour.app.data.remote.ApiResult
import com.aghour.app.data.remote.LoginRequest
import com.aghour.app.data.remote.RegisterRequest
import com.aghour.app.data.remote.RegisterUser
import com.aghour.app.data.remote.UserDto
import com.aghour.app.data.remote.safeApiCall
import com.aghour.app.data.session.SessionStore
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

    suspend fun logout() = session.clear()
}

package com.aghour.app.data.remote

import com.aghour.app.data.session.SessionStore
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds the bearer token and asks the API for Arabic messages.
 * If the server rejects our token (401), the local session is cleared.
 */
class AuthInterceptor(
    private val session: SessionStore,
    private val onUnauthorized: () -> Unit,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = session.token.value
        val request = chain.request().newBuilder()
            .header("Accept", "application/json")
            .header("Accept-Language", "ar")
            .apply { if (token != null) header("Authorization", "Bearer $token") }
            .build()

        val response = chain.proceed(request)
        if (response.code == 401 && token != null) onUnauthorized()
        return response
    }
}

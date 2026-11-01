package com.aghourservices.data.remote

import com.aghourservices.data.session.SessionStore
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds the bearer token, installation ID, and asks the API for Arabic messages.
 * If the server rejects our token (401), the local session is cleared.
 */
class AuthInterceptor(
    private val session: SessionStore,
    private val guestTokenProvider: () -> String?,
    private val installationIdProvider: () -> String?,
    private val onUnauthorized: () -> Unit,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestUrl = chain.request().url.encodedPath
        val isAuthEndpoint = requestUrl.contains("auth/guest") || requestUrl.contains("auth/login") || requestUrl.contains("auth/register")

        var token = session.token.value
        if (!isAuthEndpoint && token == null && session.user.value == null) {
            token = guestTokenProvider()
        }

        val installationId = installationIdProvider()
        val request = chain.request().newBuilder()
            .header("Accept", "application/json")
            .header("Accept-Language", "ar")
            .apply {
                if (token != null) header("Authorization", "Bearer $token")
                if (installationId != null) header("X-Installation-Id", installationId)
            }
            .build()

        val response = chain.proceed(request)
        if (response.code == 401 && token != null) onUnauthorized()
        return response
    }
}

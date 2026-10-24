package com.aghour.app.di

import android.content.Context
import com.aghour.app.BuildConfig
import com.aghour.app.data.remote.AghourApi
import com.aghour.app.data.remote.AuthInterceptor
import com.aghour.app.data.repository.AuthRepository
import com.aghour.app.data.repository.CatalogRepository
import com.aghour.app.data.session.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Simple manual dependency injection: one instance per app.
 * Can be replaced by Hilt later without touching the screens much.
 */
class AppContainer(context: Context) {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val json = Json {
        ignoreUnknownKeys = true // the API may add fields at any time
        explicitNulls = false
        coerceInputValues = true
    }

    val session = SessionStore(context, json, appScope)

    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(AuthInterceptor(session) { appScope.launch { session.clear() } })
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            }
        }
        .build()

    private val api: AghourApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(okHttp)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(AghourApi::class.java)

    val catalog = CatalogRepository(api, json)
    val auth = AuthRepository(api, session, json)
}

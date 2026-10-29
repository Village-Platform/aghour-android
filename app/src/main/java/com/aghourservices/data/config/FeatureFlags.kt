package com.aghourservices.data.config

import android.content.Context
import android.os.SystemClock
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.aghourservices.data.remote.AghourApi
import com.aghourservices.data.remote.ApiResult
import com.aghourservices.data.remote.safeApiCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private val Context.featureFlagsDataStore by preferencesDataStore(name = "feature_flags")

/**
 * On/off switches from the server (GET app_config, managed in the admin dashboard), so a feature
 * can be shown or hidden without a new app release. Features fail closed: off until the server
 * says on. The last answer is kept on disk, so an offline launch shows what the server last said
 * and a fresh install with no network shows nothing.
 */
class FeatureFlags(
    context: Context,
    private val api: AghourApi,
    private val json: Json,
    private val scope: CoroutineScope,
) {
    private val store = context.applicationContext.featureFlagsDataStore

    /**
     * Plans, subscribing and payment history. null while the saved value loads from disk: hide the
     * entry points (treat as off), but don't close a screen that is already open (e.g. restored after
     * the process was killed) until the value is known.
     */
    val plansEnabled: StateFlow<Boolean?> = store.data
        .map { it[PLANS] ?: false }
        .stateIn(scope, SharingStarted.Eagerly, null)

    @Volatile private var lastFetch = 0L

    /** Asks the server again, at most every 5 minutes (the API caches its answer for that long). */
    fun refresh() {
        val now = SystemClock.elapsedRealtime()
        if (lastFetch != 0L && now - lastFetch < REFRESH_MS) return
        lastFetch = now
        scope.launch {
            when (val result = safeApiCall(json) { api.appConfig().data.features }) {
                // A flag the server does not send is off.
                is ApiResult.Success -> store.edit { it[PLANS] = result.data[PLANS_KEY] ?: false }
                is ApiResult.Failure -> lastFetch = 0L // keep the saved value; try again next time
            }
        }
    }

    private companion object {
        const val REFRESH_MS = 5 * 60 * 1000L
        const val PLANS_KEY = "plans"
        val PLANS = booleanPreferencesKey(PLANS_KEY)
    }
}

package com.aghourservices.data.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Analytics abstraction interface following the Adapter / Service Facade pattern.
 * Decouples business logic and UI from Firebase SDK singletons.
 */
interface AnalyticsService {
    fun logScreenView(screenName: String)
    fun logEvent(name: String, params: Map<String, Any> = emptyMap())
}

class FirebaseAnalyticsService(context: Context) : AnalyticsService {
    private val firebaseAnalytics = try {
        FirebaseAnalytics.getInstance(context)
    } catch (_: Throwable) {
        null
    }

    override fun logScreenView(screenName: String) {
        try {
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            }
            firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
        } catch (_: Throwable) {
        }
    }

    override fun logEvent(name: String, params: Map<String, Any>) {
        try {
            val bundle = Bundle().apply {
                params.forEach { (k, v) ->
                    when (v) {
                        is String -> putString(k, v)
                        is Long -> putLong(k, v)
                        is Int -> putInt(k, v)
                        is Double -> putDouble(k, v)
                        is Boolean -> putBoolean(k, v)
                    }
                }
            }
            firebaseAnalytics?.logEvent(name, bundle)
        } catch (_: Throwable) {
        }
    }
}

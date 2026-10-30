package com.aghourservices.data.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Analytics abstraction interface following the Adapter / Service Facade pattern.
 * Decouples business logic and UI from Firebase SDK singletons. Events come only from the
 * [AnalyticsEvent] tracking plan, so a typo can't create a new event in the dashboard.
 */
interface AnalyticsService {
    fun logScreenView(screenName: String)
    fun log(event: AnalyticsEvent)

    /** null on logout. Our numeric user id only: never a phone or an email. */
    fun setUserId(userId: String?)

    /** "user_role": guest, user, firm_admin or super_admin, to split every report by role. */
    fun setUserRole(role: String)
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
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
            }
            firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
        } catch (_: Throwable) {
        }
    }

    override fun log(event: AnalyticsEvent) {
        try {
            val bundle = Bundle().apply {
                event.params.forEach { (k, v) ->
                    when (v) {
                        // GA4 drops string values longer than 100 characters.
                        is String -> putString(k, v.take(MAX_VALUE_LENGTH))
                        is Long -> putLong(k, v)
                        is Int -> putLong(k, v.toLong())
                        is Double -> putDouble(k, v)
                        is Boolean -> putString(k, v.toString())
                        null -> Unit // optional param not known: leave it out
                    }
                }
            }
            firebaseAnalytics?.logEvent(event.name, bundle)
        } catch (_: Throwable) {
        }
    }

    override fun setUserId(userId: String?) {
        try {
            firebaseAnalytics?.setUserId(userId)
        } catch (_: Throwable) {
        }
    }

    override fun setUserRole(role: String) {
        try {
            firebaseAnalytics?.setUserProperty("user_role", role)
        } catch (_: Throwable) {
        }
    }

    private companion object {
        const val MAX_VALUE_LENGTH = 100
    }
}

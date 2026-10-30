package com.aghourservices.data.analytics

import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Crash reporting abstraction interface following the Adapter / Service Facade pattern.
 * Decouples app code from Firebase Crashlytics static singletons.
 */
interface CrashReportingService {
    fun log(message: String)
    fun setUserId(userId: String)
    fun recordException(throwable: Throwable)
}

class FirebaseCrashReportingService : CrashReportingService {
    private val crashlytics = try {
        FirebaseCrashlytics.getInstance()
    } catch (_: Throwable) {
        null
    }

    override fun log(message: String) {
        try {
            crashlytics?.log(message)
        } catch (_: Throwable) {
        }
    }

    override fun setUserId(userId: String) {
        try {
            crashlytics?.setUserId(userId)
        } catch (_: Throwable) {
        }
    }

    override fun recordException(throwable: Throwable) {
        try {
            crashlytics?.recordException(throwable)
        } catch (_: Throwable) {
        }
    }
}

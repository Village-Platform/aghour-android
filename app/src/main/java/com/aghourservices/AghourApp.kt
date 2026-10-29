package com.aghourservices

import android.app.Application
import com.aghourservices.di.AppContainer

class AghourApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

package com.aghour.app

import android.app.Application
import com.aghour.app.di.AppContainer

class AghourApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

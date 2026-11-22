package com.vidrierapp

import android.app.Application
import com.vidrierapp.di.AppContainer

class VidrierAppApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

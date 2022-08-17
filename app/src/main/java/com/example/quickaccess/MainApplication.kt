package com.example.quickaccess

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import utils.PreferenceHelper

val prefs: PreferenceHelper by lazy {
    MainApplication.prefs!!
}

@HiltAndroidApp
class MainApplication : Application() {

    companion object {
        var prefs: PreferenceHelper? = null
        lateinit var instance: MainApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()

        instance = this

        prefs = PreferenceHelper(applicationContext)

    }

}
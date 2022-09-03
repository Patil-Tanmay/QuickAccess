package com.example.quickaccess

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.core.graphics.drawable.toBitmap
import com.example.quickaccess.data.AppDetails
import dagger.hilt.android.HiltAndroidApp
import com.example.quickaccess.utils.PreferenceHelper

val prefs: PreferenceHelper by lazy {
    MainApplication.prefs!!
}

@HiltAndroidApp
class MainApplication : Application() {

    companion object {
        var prefs: PreferenceHelper? = null
        lateinit var instance: MainApplication
            private set

        lateinit var listOfAppsPaged : List<List<AppDetails>>

        //temporary working solution
        lateinit var listOfApps: List<AppDetails>

        var pages : Int = 0
    }

    override fun onCreate() {
        super.onCreate()

        instance = this
        prefs = PreferenceHelper(applicationContext)

    }

}
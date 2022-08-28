package com.example.quickaccess

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
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
        listOfApps =
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter {
                    packageManager.getLaunchIntentForPackage(it.packageName) != null
                }.map { applicationInfo ->
                    AppDetails(
                        packageName = applicationInfo.packageName,
                        name = packageManager.getApplicationLabel(applicationInfo).toString(),
                        image = applicationInfo.loadIcon(packageManager),
                        isSystemPackage = isSystemPackage(applicationInfo)
                    )
                }

        listOfAppsPaged = listOfApps.chunked(20)

        pages = listOfAppsPaged.size
        prefs = PreferenceHelper(applicationContext)

    }

    fun refreshAppList(){
        listOfAppsPaged = listOf(listOfAppsPaged[0])
    }

    private fun isSystemPackage(pkgInfo: ApplicationInfo): Boolean {
        return pkgInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
    }

}
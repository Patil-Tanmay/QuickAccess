package com.example.quickaccess.service

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.service.quicksettings.TileService
import android.util.Base64
import androidx.annotation.RequiresApi
import com.example.quickaccess.R
import com.example.quickaccess.utils.PreferenceHelper


@RequiresApi(Build.VERSION_CODES.N)
class QuickAccessService : TileService() {

    companion object {
        @JvmStatic var isTileAdded: Boolean = false
    }

    private lateinit var prefs: PreferenceHelper

    private lateinit var quickAccessPackageName: String

    override fun onClick() {
        super.onClick()
        val intent = packageManager.getLaunchIntentForPackage(quickAccessPackageName)
        intent?.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivityAndCollapse(intent)
    }

    override fun onStartListening() {
        prefs = PreferenceHelper(baseContext)
       val listOfApps =
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter {
                    packageManager.getLaunchIntentForPackage(it.packageName) != null
                }.map { applicationInfo ->
                    AppName(
                        packageManager.getApplicationLabel(applicationInfo).toString(),
                        applicationInfo.packageName
                    )
                }
        if (listOfApps.map { it.packageName }.contains(prefs.quickAccessAppName)) {
            val imageInfo = listOfApps.map { it.packageName }.indexOf(prefs.quickAccessAppName)
            qsTile.label = listOfApps[imageInfo].name
            quickAccessPackageName = listOfApps[imageInfo].packageName
            qsTile.updateTile()
        }else{
            qsTile.label = "Quick Access"
            quickAccessPackageName = applicationInfo.packageName
            qsTile.updateTile()
        }
        super.onStartListening()
    }

    override fun onTileAdded() {
        isTileAdded = true
        super.onTileAdded()
    }

    override fun onTileRemoved() {
        isTileAdded = false
        super.onTileRemoved()
    }

    override fun onStopListening() {
        super.onStopListening()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun decodeToBitmap(): Bitmap {
        val encodedImage = prefs.imageDrawable
        val b: ByteArray = Base64.decode(encodedImage, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(b, 0, b.size)
        return bitmap
    }

    data class AppName(
        val name : String,
        val packageName : String
    )
}
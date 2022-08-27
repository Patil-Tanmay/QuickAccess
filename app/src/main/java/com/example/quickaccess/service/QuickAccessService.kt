package com.example.quickaccess.service

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.TileService
import android.util.Base64
import androidx.annotation.RequiresApi
import com.example.quickaccess.utils.PreferenceHelper


@RequiresApi(Build.VERSION_CODES.N)
class QuickAccessService : TileService() {

    private lateinit var prefs: PreferenceHelper

    override fun onClick() {
        super.onClick()
        val intent = packageManager.getLaunchIntentForPackage(prefs.quickAccessAppName!!)
        intent?.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivityAndCollapse(intent)
    }

    override fun onStartListening() {
        prefs = PreferenceHelper(baseContext)
        super.onStartListening()
        qsTile.icon = Icon.createWithBitmap(decodeToBitmap())
        qsTile.updateTile()
    }

    override fun onTileAdded() {
        super.onTileAdded()
    }

    private fun decodeToBitmap(): Bitmap {
        val encodedImage = prefs.imageDrawable
        val b: ByteArray = Base64.decode(encodedImage, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(b, 0, b.size)
        return bitmap
    }

}
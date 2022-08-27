package com.example.quickaccess.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.example.quickaccess.R
import com.example.quickaccess.utils.PreferenceHelper

@RequiresApi(Build.VERSION_CODES.N)
class QuickAccessService : TileService() {

    override fun onClick() {
        super.onClick()
        val prefs : PreferenceHelper = PreferenceHelper(baseContext)
        val intent = packageManager.getLaunchIntentForPackage(prefs.quickAccessAppName!!)
        intent?.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivityAndCollapse(intent)
    }

    override fun onStartListening() {
        super.onStartListening()
    }

    override fun onTileAdded() {
        super.onTileAdded()
    }

}
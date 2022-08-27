package com.example.quickaccess.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.quickaccess.utils.APPCONSTANTS.APPDRAWABLE
import com.example.quickaccess.utils.APPCONSTANTS.ISDARKTHEME
import com.example.quickaccess.utils.APPCONSTANTS.PACKAGENAME
import com.example.quickaccess.utils.APPCONSTANTS.SHARED_PREF

class PreferenceHelper(context : Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE)

    var quickAccessAppName : String?
    get() = prefs.getString(PACKAGENAME, "com.example.quickaccess")
    set(value) = prefs.edit().putString(PACKAGENAME, value).apply()

    var isDarkTheme : Boolean
    get() = prefs.getBoolean(ISDARKTHEME, false)
    set(value) = prefs.edit().putBoolean(ISDARKTHEME, value).apply()

    var imageDrawable : String?
        get() = prefs.getString(APPDRAWABLE, "randomText")
        set(value) = prefs.edit().putString(APPDRAWABLE, value).apply()

    //todo convert actual logo into base64 for default value

}

object APPCONSTANTS{
    const val SHARED_PREF = "SHARED_PREF"

    const val ISDARKTHEME = "DARKTHEME"

    const val PACKAGENAME = "PACKAGENAME"

    const val APPDRAWABLE = "AppDrawable"

}
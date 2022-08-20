package utils

import android.content.Context
import android.content.SharedPreferences
import utils.APPCONSTANTS.ISDARKTHEME
import utils.APPCONSTANTS.PACKAGENAME
import utils.APPCONSTANTS.SHARED_PREF

class PreferenceHelper(context : Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE)

    var quickAccessAppName : String?
    get() = prefs.getString(PACKAGENAME, "com.example.quickaccess")
    set(value) = prefs.edit().putString(PACKAGENAME, value).apply()

    var isDarkTheme : Boolean
    get() = prefs.getBoolean(ISDARKTHEME, false)
    set(value) = prefs.edit().putBoolean(ISDARKTHEME, value).apply()

}

object APPCONSTANTS{
    const val SHARED_PREF = "SHARED_PREF"

    const val ISDARKTHEME = "DARKTHEME"

    const val PACKAGENAME = "PACKAGENAME"

}
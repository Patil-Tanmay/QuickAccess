package utils

import android.content.Context
import android.content.SharedPreferences
import utils.APP_CONSTANTS.PACKAGENAME
import utils.APP_CONSTANTS.SHARED_PREF

class PreferenceHelper(context : Context) {

    val prefs: SharedPreferences = context.getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE)

    var quickAccessAppName : String?
    get() = prefs.getString(PACKAGENAME, "com.example.quickaccess")
    set(value) = prefs.edit().putString(PACKAGENAME, value)
    .apply()


}

object APP_CONSTANTS{
    const val SHARED_PREF = "SHARED_PREF"

    const val PACKAGENAME = "PACKAGENAME"

}
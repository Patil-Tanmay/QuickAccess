package com.example.quickaccess.data

import android.graphics.drawable.Drawable

data class AppDetails(
    val id : Int?=null,
    val name : String,
    val packageName : String,
    val image : Drawable,
    val isSystemPackage : Boolean
) {
}
package com.example.quickaccess.data

import android.graphics.Bitmap

data class AppDetails(
    val id : Int?=null,
    val name : String,
    val packageName : String,
    val image : Bitmap,
    val isSystemPackage : Boolean
)
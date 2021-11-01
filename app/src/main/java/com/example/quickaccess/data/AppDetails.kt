package com.example.quickaccess.data

import android.graphics.drawable.Drawable
import android.media.Image
import androidx.room.Entity
import androidx.room.PrimaryKey

data class AppDetails(
    val id : Int?=null,
    val name : String,
    val packageName : String,
    val image : Drawable
) {
}
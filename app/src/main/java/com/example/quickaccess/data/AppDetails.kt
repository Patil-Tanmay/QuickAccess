package com.example.quickaccess.data

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import com.example.quickaccess.utils.UiState
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.flow.StateFlow

@Parcelize
data class AppDetails(
    val id : Int?=null,
    val name : String,
    val packageName : String,
    val image : Bitmap,
    val isSystemPackage : Boolean
): Parcelable

data class AppListing(
    val appList: LiveData<PagedList<AppDetails>>,
    val refreshState: StateFlow<UiState>,
    val onRefresh: (genreNme: String?, isRefresh: Boolean) -> Unit
)
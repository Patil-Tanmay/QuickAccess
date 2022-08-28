package com.example.quickaccess.data

import android.graphics.drawable.Drawable
import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import com.example.quickaccess.utils.UiState
import kotlinx.coroutines.flow.StateFlow

data class AppDetails(
    val id : Int?=null,
    val name : String,
    val packageName : String,
    val image : Drawable,
    val isSystemPackage : Boolean
)

data class AppListing(
    val appList: LiveData<PagedList<AppDetails>>,
    val refreshState: StateFlow<UiState>,
    val onRefresh: (genreNme: String?, isRefresh: Boolean) -> Unit
)
package com.example.quickaccess.data

import androidx.paging.PageKeyedDataSource
import com.example.quickaccess.MainApplication.Companion.listOfAppsPaged
import com.example.quickaccess.MainApplication.Companion.pages
import com.example.quickaccess.utils.UiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class AppDataSource(
    private val scope: CoroutineScope,
    private val initLoadState: MutableStateFlow<UiState>,
) : PageKeyedDataSource<Int, AppDetails>() {

    override fun loadInitial(
        params: LoadInitialParams<Int>,
        callback: LoadInitialCallback<Int, AppDetails>
    ) {
        scope.launch {
            initLoadState.emit(UiState.Loading)
            callback.onResult(listOfAppsPaged[0], null,1)
            initLoadState.emit(UiState.Success)
        }
    }

    override fun loadBefore(params: LoadParams<Int>, callback: LoadCallback<Int, AppDetails>) {
        scope.launch {
            if (params.key <= pages){
                val nextPageKey = params.key + 1
                callback.onResult(listOfAppsPaged[params.key],nextPageKey)
            }
        }
    }

    override fun loadAfter(params: LoadParams<Int>, callback: LoadCallback<Int, AppDetails>) {
        TODO("Not yet implemented")
    }
}
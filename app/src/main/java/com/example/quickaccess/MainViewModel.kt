package com.example.quickaccess

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.*
import androidx.paging.DataSource
import androidx.paging.PagedList
import androidx.paging.toLiveData
import com.example.quickaccess.MainApplication.Companion.listOfAppsPaged
import com.example.quickaccess.data.AppDataSource
import com.example.quickaccess.data.AppDetails
import com.example.quickaccess.data.AppListing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.example.quickaccess.utils.Resource
import com.example.quickaccess.utils.UiState
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(val app: Application) : AndroidViewModel(app) {

    private var _appList: MutableSharedFlow<Resource<List<AppDetails>>> = MutableSharedFlow(1)
    val appListFlow: MutableSharedFlow<Resource<List<AppDetails>>> get() = _appList

    private var appList = arrayListOf<AppDetails>()

    private var currentQuery: String? = null

    init {
//        getList(app.packageManager)
    }

    private fun getList(packageManager: PackageManager) {
        viewModelScope.launch {
            _appList.emit(Resource.Loading)
            if (currentQuery != null) {
                appList.clear()
                appList.addAll(MainApplication.listOfApps)
                filterAppList(currentQuery!!)
            } else {
                appList.clear()
                appList.addAll(MainApplication.listOfApps)
                _appList.emit(Resource.Success(MainApplication.listOfApps))
            }
        }
    }

    fun getPagedAppList(): AppListing {
        val sourceFactory = AppDataSourceFactory(
            scope = viewModelScope,
            searchString = ""
        )

        val pageConfig = PagedList.Config.Builder()
            .setPageSize(20)
            .setEnablePlaceholders(false)
            .setPrefetchDistance(10)
            .build()


        return AppListing(
            appList = sourceFactory.toLiveData(pageConfig),
            refreshState = sourceFactory.initLoadState,
            onRefresh = { searchString, isRefresh ->
                if (isRefresh) {
                    sourceFactory.source?.invalidate()
                } else {
                    sourceFactory.search = searchString ?: ""
                    sourceFactory.source?.invalidate()
                }
            }
        )
    }

    fun filterAppList(appName: String) {
        viewModelScope.launch {
            if (appName != "") {
                val newList = appList.filter {
                    it.name.contains(appName, true)
                }
                setQuery(appName)
                _appList.emit(Resource.Success(newList))
            } else {
                setQuery(null)
                _appList.emit(Resource.Success(appList))
            }
        }
    }

    fun setQuery(query: String?) {
        currentQuery = query
    }

    fun onRefresh() {
        getList(app.packageManager)
    }

    private fun isSystemPackage(pkgInfo: ApplicationInfo): Boolean {
        return pkgInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
    }
}

class AppDataSourceFactory(
    private val scope: CoroutineScope,
    searchString: String
) : DataSource.Factory<Int, AppDetails>() {

    val initLoadState = MutableStateFlow(UiState.Loading)

    var source: AppDataSource? = null
        private set

    var search = searchString

    override fun create(): DataSource<Int, AppDetails> {
        val searchString = search
        val appDataSource = AppDataSource(
            scope = scope,
            initLoadState = initLoadState
        )
        source = appDataSource
        return appDataSource
    }

}
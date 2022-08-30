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

    private var appListPaged = arrayListOf<List<AppDetails>>()

    private var totalPages: Int = 0

    var currentQuery: String? = null

    private var currentPageNo: Int = 0

    private var searchPageNo: Int = 0

    private var searchPages : Int = 0

    private var currentPagedList = arrayListOf<AppDetails>()

    private var searchListPaged = arrayListOf<List<AppDetails>>()

    init {
        getList(app.packageManager, 0)
    }

    private fun getList(packageManager: PackageManager, pageNo: Int) {
        viewModelScope.launch {
            _appList.emit(Resource.Loading)
            val listOfApps =
                packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                    .filter {
                        packageManager.getLaunchIntentForPackage(it.packageName) != null
                    }.map { applicationInfo ->
                        AppDetails(
                            packageName = applicationInfo.packageName,
                            name = packageManager.getApplicationLabel(applicationInfo).toString(),
                            image = applicationInfo.loadIcon(packageManager),
                            isSystemPackage = isSystemPackage(applicationInfo)
                        )
                    }
            currentPageNo = pageNo
            appListPaged.clear()
            appListPaged.addAll(listOfApps.chunked(20))
            totalPages = appListPaged.lastIndex
            currentPagedList.clear()
            currentPagedList.addAll(appListPaged[pageNo])

            appList.clear()
            appList.addAll(listOfApps)
            _appList.emit(Resource.Success(appListPaged[currentPageNo]))
        }


        //old Working
//        viewModelScope.launch {
//            _appList.emit(Resource.Loading)
//            if (currentQuery != null) {
//                appList.clear()
//                appList.addAll(MainApplication.listOfApps)
//                filterAppList(currentQuery!!)
//            } else {
//                appList.clear()
//                appList.addAll(MainApplication.listOfApps)
//                _appList.emit(Resource.Success(MainApplication.listOfApps))
//            }
//        }
    }

    fun getNextPagedList() {
        viewModelScope.launch {
            if (currentPageNo < totalPages) {
                currentPageNo += 1
                currentPagedList.addAll(appListPaged[currentPageNo])
                _appList.emit(Resource.Success(currentPagedList))
            }
        }
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

    fun filterAppListPaged(appName: String) {
        viewModelScope.launch {
            if (appName != "") {
                searchPageNo = 0
                searchListPaged.clear()
                val newList = appList.filter {
                    it.name.contains(appName, true)
                }.chunked(20)
                searchPages = newList.lastIndex
                searchListPaged.addAll(newList)
                searchPageNo += 1
                setQuery(appName)
                _appList.emit(Resource.Success(newList[0]))
            } else {
//                currentPageNo = 1
                setQuery(null)
                getList(packageManager = app.packageManager, 0)
//                _appList.emit(Resource.Success(appListPaged[0]))
            }
        }
    }

    fun filterAppListNextPage() {
        viewModelScope.launch {
            if (searchPageNo < searchPages) {
                _appList.emit(Resource.Success(searchListPaged[searchPageNo]))
                searchPageNo += 1
            }
        }
    }


    fun setQuery(query: String?) {
        currentQuery = query
    }

    fun onRefresh() {
        getList(app.packageManager, 0)
    }

    private fun isSystemPackage(pkgInfo: ApplicationInfo): Boolean {
        return pkgInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
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
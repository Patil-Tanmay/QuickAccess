package com.example.quickaccess

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.core.graphics.drawable.toBitmap
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
import kotlinx.coroutines.channels.Channel
import javax.inject.Inject

data class AppListingPaged(
    var currentPageNo: Int = 0,
    var totalPages : Int = 0,
    var appListPaged: ArrayList<List<AppDetails>> = arrayListOf(),
    var currentPagedList: ArrayList<AppDetails> = arrayListOf(),
    var appList : ArrayList<AppDetails> = arrayListOf()
)

@HiltViewModel
class MainViewModel @Inject constructor(val app: Application) : AndroidViewModel(app) {

    private var _appList: MutableSharedFlow<Resource<List<AppDetails>>> = MutableSharedFlow(1)
    val appListFlow: MutableSharedFlow<Resource<List<AppDetails>>> get() = _appList

    private var appList = arrayListOf<AppDetails>()

    private lateinit var appListingPaged : AppListingPaged

    private var appListPaged = arrayListOf<List<AppDetails>>()

    private var totalPages: Int = 0

    var currentQuery: String? = null

    private var currentPageNo: Int = 0

    private var searchPageNo: Int = 0

    private var searchPages: Int = 0

    private var currentPagedList = arrayListOf<AppDetails>()

    private var currentPagedFilterList = arrayListOf<AppDetails>()

    private var searchListPaged = arrayListOf<List<AppDetails>>()

    init {
        getList(app.packageManager, 0)
    }

    private fun resetAppListing(){
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
                            image = applicationInfo.loadIcon(packageManager).toBitmap(),
                            isSystemPackage = isSystemPackage(applicationInfo)
                        )
                    }
            currentPageNo = pageNo
            appListPaged.clear()
            appListPaged.addAll(listOfApps.chunked(10))
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

    fun filterAppListPaged(appName: String, toBeRefreshed: Boolean) {
        viewModelScope.launch {
            _appList.emit(Resource.Loading)
            if (appName != "") {
                searchPageNo = 0
                currentPagedFilterList.clear()
                searchListPaged.clear()
                val newList = if (toBeRefreshed) {
                    getAppList().filter {
                        it.name.contains(appName, true)
                    }.chunked(20)
                } else {
                    appList.filter {
                        it.name.contains(appName, true)
                    }.chunked(20)
                }
                if (newList.isEmpty()) {
                    _appList.emit(Resource.Success(currentPagedFilterList))
                }else{
                    searchPages = newList.lastIndex
                    searchListPaged.addAll(newList)
                    currentPagedFilterList.addAll(newList[0])
                    searchPageNo += 1
                    setQuery(appName)
                    _appList.emit(Resource.Success(newList[0]))
                }
            } else {
                setQuery(null)
                getList(packageManager = app.packageManager, 0)
            }
        }
    }

    private fun getAppList(): List<AppDetails> {
        val pm = app.packageManager

        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter {
                pm.getLaunchIntentForPackage(it.packageName) != null
            }.map { applicationInfo ->
                AppDetails(
                    packageName = applicationInfo.packageName,
                    name = pm.getApplicationLabel(applicationInfo).toString(),
                    image = applicationInfo.loadIcon(pm).toBitmap(),
                    isSystemPackage = isSystemPackage(applicationInfo)
                )
            }
    }

    fun filterAppListNextPage() {
        viewModelScope.launch {
            if (searchPageNo < searchPages) {
                currentPagedFilterList.addAll(searchListPaged[searchPageNo])
                _appList.emit(Resource.Success(currentPagedFilterList))
                searchPageNo += 1
            }
        }
    }


    fun setQuery(query: String?) {
        currentQuery = query
    }

    fun onRefresh() {
        if (currentQuery == null) {
            getList(app.packageManager, 0)
        } else {
            filterAppListPaged(currentQuery!!, true)
        }
    }

    private var currentAppForUnInstall: AppDetails? = null
    var currentAppForUnInstallPosition: Int = 0
    private var _updateAppListAfterUnInstall  = Channel<List<AppDetails>>()
    val updateAppListAfterUnInstall get() =  _updateAppListAfterUnInstall.receiveAsFlow()
    fun setAppForUnInstall(app: AppDetails, position: Int) {
        currentAppForUnInstall = app
        currentAppForUnInstallPosition = position
    }

    fun onUnInstall(unInstalled: Boolean) {
        viewModelScope.launch {
            if (unInstalled) {
                if (!currentAppForUnInstall?.isSystemPackage!!) {
                    if (currentQuery == null) {
                        appList.remove(currentAppForUnInstall!!)
                        currentPagedList.remove(currentAppForUnInstall)
                        _updateAppListAfterUnInstall.send(currentPagedList!!)
//                        currentAppForUnInstall = null
//                        _appList.emit(Resource.Success(currentPagedList))
                    } else {
                        appList.remove(currentAppForUnInstall!!)
                        currentPagedFilterList.remove(currentAppForUnInstall)
                        _appList.emit(Resource.Success(currentPagedFilterList))
                    }
                }
            }else{
                currentAppForUnInstall = null
            }
        }
    }

    private fun isSystemPackage(pkgInfo: ApplicationInfo): Boolean {
        return pkgInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
    }

    //paging 2 Impl
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
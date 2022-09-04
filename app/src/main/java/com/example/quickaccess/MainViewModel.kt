package com.example.quickaccess

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.*
import com.example.quickaccess.data.AppDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import com.example.quickaccess.utils.Resource
import com.example.quickaccess.utils.parallelMap
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
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

    private var searchPages: Int = 0

    private var currentPagedList = arrayListOf<AppDetails>()

    private var currentPagedFilterList = arrayListOf<AppDetails>()

    private var searchListPaged = arrayListOf<List<AppDetails>>()

    private var _totalApps = MutableStateFlow(0)
    val totalApps : StateFlow<Int> get() = _totalApps

    private var _closeSearchView = Channel<Boolean>()
    val  closeSearchView = _closeSearchView.receiveAsFlow()

    var isSearchViewOpen = false

    init {
        viewModelScope.launch {
            getInitialData(app.packageManager, 0)
        }
    }

    fun closeSearchView(){
        viewModelScope.launch {
            _closeSearchView.send(true)
        }
    }

    private fun getInitialData(packageManager: PackageManager, pageNo: Int) {
        viewModelScope.launch {
            _appList.emit(Resource.Loading)

            val listOfApps = getList(packageManager)
            _totalApps.emit(listOfApps.size)
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
    }

    private suspend fun getList(packageManager: PackageManager) =
        withContext(Dispatchers.IO) {
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter {
                    packageManager.getLaunchIntentForPackage(it.packageName) != null
                }.parallelMap { applicationInfo ->
                    AppDetails(
                        packageName = applicationInfo.packageName,
                        name = packageManager.getApplicationLabel(applicationInfo).toString(),
                        image = applicationInfo.loadIcon(packageManager).toBitmap(),
                        isSystemPackage = isSystemPackage(applicationInfo)
                    )
                }
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

    fun filterAppListPaged(appName: String, toBeRefreshed: Boolean) {
        viewModelScope.launch {
            _appList.emit(Resource.Loading)
            if (appName != "") {
                searchPageNo = 0
                currentPagedFilterList.clear()
                searchListPaged.clear()
                val newList = if (toBeRefreshed) {
                    val listOfApps = getList(app.packageManager)
                    appList.clear()
                    appList.addAll(listOfApps)
                    listOfApps.filter {
                        it.name.contains(appName, true)
                    }.chunked(20)
                } else {
                    appList.filter {
                        it.name.contains(appName, true)
                    }.chunked(20)
                }
                if (newList.isEmpty()) {
                    _appList.emit(Resource.Success(currentPagedFilterList))
                } else {
                    searchPages = newList.lastIndex
                    searchListPaged.addAll(newList)
                    currentPagedFilterList.addAll(newList[0])
                    searchPageNo += 1
                    setQuery(appName)
                    _appList.emit(Resource.Success(newList[0]))
                }
            } else {
                setQuery(null)
                getInitialData(packageManager = app.packageManager, 0)
            }
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
            getInitialData(app.packageManager, 0)
        } else {
            filterAppListPaged(currentQuery!!, true)
        }
    }

    private var currentAppForUnInstall: AppDetails? = null
    var currentAppForUnInstallPosition: Int = 0
    private var _updateAppListAfterUnInstall = Channel<List<AppDetails>>()
    val updateAppListAfterUnInstall get() = _updateAppListAfterUnInstall.receiveAsFlow()
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
                        _updateAppListAfterUnInstall.send(currentPagedList)
//                        currentAppForUnInstall = null
//                        _appList.emit(Resource.Success(currentPagedList))
                    } else {
                        appList.remove(currentAppForUnInstall!!)
                        currentPagedFilterList.remove(currentAppForUnInstall)
                        _appList.emit(Resource.Success(currentPagedFilterList))
                    }
                }
            } else {
                currentAppForUnInstall = null
            }
        }
    }

    private fun isSystemPackage(pkgInfo: ApplicationInfo): Boolean {
        return pkgInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
    }
}
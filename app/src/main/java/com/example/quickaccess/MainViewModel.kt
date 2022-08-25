package com.example.quickaccess

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.*
import com.example.quickaccess.data.AppDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.example.quickaccess.utils.Resource
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(val app: Application) : AndroidViewModel(app) {

    private var _appList: MutableSharedFlow<Resource<List<AppDetails>>> = MutableSharedFlow(1)
    val appListFlow: MutableSharedFlow<Resource<List<AppDetails>>> get() = _appList

    private var appList = arrayListOf<AppDetails>()

    private var currentQuery : String ?= null

    init {
        getList(app.packageManager)
    }

    private fun getList(packageManager: PackageManager) {
        viewModelScope.launch {
            _appList.emit(Resource.Loading)
            val listOfApps =
                packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                    .filter {
                        packageManager.getLaunchIntentForPackage(it.packageName) != null
                    }.map { applicationInfo ->
                        AppDetails(
                            packageName = applicationInfo.packageName,
                            name = packageManager.getApplicationLabel(applicationInfo)
                                .toString(),
                            image = applicationInfo.loadIcon(packageManager),
                            isSystemPackage = isSystemPackage(applicationInfo)
                        )
                    }
            if (currentQuery != null){
                appList.clear()
                appList.addAll(listOfApps)
                filterAppList(currentQuery!!)
            }else {
                appList.clear()
                appList.addAll(listOfApps)
                _appList.emit(Resource.Success(listOfApps))
            }
        }
    }

    fun filterAppList(appName: String){
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

     fun setQuery(query: String?){
        currentQuery = query
    }

    fun onRefresh() {
        getList(app.packageManager)
    }

    private fun isSystemPackage(pkgInfo: ApplicationInfo): Boolean {
        return pkgInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
    }


}
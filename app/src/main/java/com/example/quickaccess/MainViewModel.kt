package com.example.quickaccess

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.quickaccess.data.AppDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
//    val db : AppDatabase
) : AndroidViewModel(application) {

    private var _refreshStatus: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val refreshStatus get() = _refreshStatus.asStateFlow()

    private val _liveDate = MutableLiveData<List<AppDetails>>()
    val liveData: LiveData<List<AppDetails>>
        get() = _liveDate

    init {
        viewModelScope.launch {
            _liveDate.value = getList(application.packageManager)
        }
    }

    private suspend fun getList(packageManager: PackageManager) = withContext(Dispatchers.IO) {
        packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { packageManager.getLaunchIntentForPackage(it.packageName) != null }
            .parallelMap {
                AppDetails(
                    packageName = it.packageName,
                    name = packageManager.getApplicationLabel(it).toString(),
                    image = it.loadIcon(packageManager),
                    isSystemPackage = false
                )
            }
    }


    private suspend fun <A, B> Iterable<A>.parallelMap(f: suspend (A) -> B): List<B> =
        coroutineScope {
            map { async { f(it) } }.awaitAll()
        }

    fun getInstalledApps() =
        _refreshStatus.flatMapLatest { refresh ->
            val list = mutableListOf<AppDetails>()
            flow {
                if (refresh) {
//                    list.clear()
                    val application = getApplication<Application>()
                    val pm = application.packageManager
                    val packs = pm.getInstalledPackages(PackageManager.GET_META_DATA)
                    for (i in packs.indices) {
                        val p = packs[i]
                        if (!isSystemPackage(p)) {
                            list.add(
                                AppDetails(
                                    name = p.applicationInfo.loadLabel(pm).toString(),
                                    packageName = p.applicationInfo.packageName.toString(),
                                    image = p.applicationInfo.loadIcon(pm),
                                    isSystemPackage = false
                                )
                            )
                        }
//                        else {
//                            list.add(
//                                AppDetails(
//                                    name = p.applicationInfo.loadLabel(pm).toString(),
//                                    packageName = p.applicationInfo.packageName.toString(),
//                                    image = p.applicationInfo.loadIcon(pm),
//                                    isSystemPackage = true
//                                )
//                            )
//                        }
                        emit(list.toList())
                        _refreshStatus.value = false
                    }
                }
//                else {
//                    emit(list.toList())
////                    _refreshStatus.value = false
//                }
            }
        }

    fun onRefresh() {
        _refreshStatus.value = true
    }

    fun stopRefresh() {
        _refreshStatus.value = false
    }


    private fun isSystemPackage(pkgInfo: PackageInfo): Boolean {
        return pkgInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
    }


}
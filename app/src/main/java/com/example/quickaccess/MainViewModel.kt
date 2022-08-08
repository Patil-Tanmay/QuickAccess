package com.example.quickaccess

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import com.example.quickaccess.data.AppDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    init {

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
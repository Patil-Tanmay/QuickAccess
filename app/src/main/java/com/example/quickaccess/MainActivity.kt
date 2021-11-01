package com.example.quickaccess

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.quickaccess.data.AppAdapter
import com.example.quickaccess.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val TAG = "TAGG"

    private val viewModel by viewModels<MainViewModel>()

    private val UN = 1

    private lateinit var binding: ActivityMainBinding

    private lateinit var adapter: AppAdapter

    private val result =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_CANCELED && it.data != null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = AppAdapter(
            ::onSelect,
            ::onUninstall
        )

        setupRecView(adapter)

        binding.refreshLayout.setOnRefreshListener {
            viewModel.onRefresh()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                //showing Progressbar
                launch {
                    viewModel.refreshStatus.collect {
                        Log.d(TAG, "onCreate: $it")
                        if (it) {
                            binding.pBar.visibility = View.VISIBLE
                            binding.refreshLayout.isRefreshing = true
                            binding.recView.visibility = View.GONE
                        } else {
                            binding.pBar.visibility = View.GONE
                            binding.refreshLayout.isRefreshing = false
                            binding.recView.visibility = View.VISIBLE
                        }
                    }
                }

                launch {
                    viewModel.getInstalledApps().collect {
                        Log.d(TAG, "onCreate: ${it.size}")
                        adapter.setAppData(it)
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }//end of onCreate


    //private functions

    private fun setupRecView(adapter: AppAdapter) {
        binding.recView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.recView.adapter = adapter
    }

    private fun onSelect(packageName: String) {
        Log.d(TAG, "onSelect: $packageName")
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//        startActivity(intent)
        result.launch(intent)
    }


    override fun onStart() {
        super.onStart()
        viewModel.onRefresh()
    }


    private fun onUninstall(packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE)
        intent.data = Uri.parse("package:$packageName")
        intent.putExtra(Intent.EXTRA_RETURN_RESULT, true)
        startActivity(intent)

    }

    //useless code
    //        val pm = packageManager
////get a list of installed apps.
////get a list of installed apps.
//        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
//
//        for (packageInfo in packages) {
//            Log.d(TAG, "Installed package :" + packageInfo.packageName)
//            Log.d(TAG, "Source dir : " + packageInfo.sourceDir)
//            Log.d(TAG, "Launch Activity :" + pm.getLaunchIntentForPackage(packageInfo.packageName))
//        }


}
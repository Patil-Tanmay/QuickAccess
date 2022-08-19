package com.example.quickaccess

import android.animation.Animator
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewAnimationUtils
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.quickaccess.data.AppAdapter
import com.example.quickaccess.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import utils.Resource
import utils.hideKeyboard
import utils.showKeyBoard


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val TAG = "TAGG"

    private val viewModel by viewModels<MainViewModel>()

    private val UN = 1

    private lateinit var binding: ActivityMainBinding

    private lateinit var adapter: AppAdapter

    private val result =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                viewModel.onRefresh()
                Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
            }
        }

    private val unInstallApp =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                Toast.makeText(this, "Successfully Uninstalled", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_Dark)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = AppAdapter(
            ::onSelect,
            ::onUninstall,
            ::setPackageNameForQuickAccess
        )
        setupRecView(adapter)

        binding.openSearchButton.setOnClickListener {
            binding.searchInputText.requestFocus()
            binding.searchInputText.showKeyBoard()
            openSearch()
        }
        binding.closeSearchButton.setOnClickListener {
            viewModel.setQuery(null)
            this.hideKeyboard()
            closeSearch()
        }

        binding.refreshLayout.setOnRefreshListener {
            viewModel.onRefresh()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.appListt.collect { resource ->
                        when (resource) {
                            is Resource.Loading -> {
                                binding.pBar.visibility = View.VISIBLE
                                binding.refreshLayout.isRefreshing = true
                                binding.recView.visibility = View.GONE
                            }

                            is Resource.Success -> {
                                binding.pBar.visibility = View.GONE
                                binding.refreshLayout.isRefreshing = false
                                binding.recView.visibility = View.VISIBLE

//                                Log.d(TAG, "onCreate: ${it.size}")
                                adapter.setAppData(resource.data!!)
                                adapter.notifyDataSetChanged()

                            }
                            is Resource.Error -> {
                                binding.pBar.visibility = View.GONE
                                binding.refreshLayout.isRefreshing = false
                                binding.recView.visibility = View.VISIBLE

                                Snackbar.make(
                                    binding.root,
                                    "Error! Please relaunch the app.",
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }
                        }

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
//        viewModel.onRefresh()
    }

    private fun setPackageNameForQuickAccess(packageName: String){
        prefs.quickAccessAppName = packageName
    }

    private fun onUninstall(packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE)
        intent.data = Uri.parse("package:$packageName")
        intent.putExtra(Intent.EXTRA_RETURN_RESULT, true)
        unInstallApp.launch(intent)

    }

    private fun openSearch() {
        binding.searchInputText.addTextChangedListener {
            if (!binding.searchInputText.text.isNullOrBlank()) {
                viewModel.filterAppList(it.toString())
            } else if (binding.searchInputText.text.isBlank()) {
                viewModel.filterAppList("")
            }
        }


        binding.searchOpenView.visibility = View.VISIBLE
        val circularReveal = ViewAnimationUtils.createCircularReveal(
            binding.searchOpenView,
            (binding.openSearchButton.right + binding.openSearchButton.left) / 2,
            (binding.openSearchButton.top + binding.openSearchButton.bottom) / 2,
            0f, binding.searchLayout.width.toFloat()
        )
        circularReveal.duration = 350
        circularReveal.start()
    }

    private fun closeSearch() {
        val circularConceal = ViewAnimationUtils.createCircularReveal(
            binding.searchOpenView,
            (binding.openSearchButton.right + binding.openSearchButton.left) / 2,
            (binding.openSearchButton.top + binding.openSearchButton.bottom) / 2,
            binding.searchLayout.width.toFloat(), 0f
        )

        circularConceal.duration = 350
        circularConceal.start()
        circularConceal.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) = Unit
            override fun onAnimationCancel(animation: Animator?) = Unit
            override fun onAnimationStart(animation: Animator?) = Unit
            override fun onAnimationEnd(animation: Animator?) {
                binding.searchOpenView.visibility = View.INVISIBLE
                binding.searchInputText.setText("")
                circularConceal.removeAllListeners()
            }
        })
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
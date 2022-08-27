package com.example.quickaccess.ui

import android.animation.Animator
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Log.INFO
import android.view.View
import android.view.ViewAnimationUtils
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.quickaccess.MainViewModel
import com.example.quickaccess.R
import com.example.quickaccess.data.AppAdapter
import com.example.quickaccess.data.AppDetails
import com.example.quickaccess.databinding.BottomsheetQuickSettingBinding
import com.example.quickaccess.databinding.DialogQuickSettingBinding
import com.example.quickaccess.databinding.FragmentMainBinding
import com.example.quickaccess.prefs
import com.example.quickaccess.service.QuickAccessService
import com.example.quickaccess.utils.*
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.util.logging.Level.INFO

class MainFragment : Fragment(R.layout.fragment_main) {

    private val binding by viewBinding(FragmentMainBinding::bind)

    private lateinit var adapter: AppAdapter

    private val viewModel by viewModels<MainViewModel>()

    private lateinit var dialogQuickSettingBinding: DialogQuickSettingBinding
    private lateinit var dialog: AlertDialog

    interface OnThemeChangeCallBack {
        fun onThemeChanged(isChanged: Boolean)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (prefs.isDarkTheme) {
            requireContext().theme.applyStyle(R.style.Theme_Dark, true)

            requireActivity().window.setBackgroundDrawable(
                ColorDrawable(ContextCompat.getColor(requireContext(), R.color.backgroundColor))
            )
        } else {
            requireContext().theme.applyStyle(R.style.Theme_QuickAccess, true)

            requireActivity().window.setBackgroundDrawable(
                ColorDrawable(ContextCompat.getColor(requireContext(), R.color.white))
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.startCircularReveal()
        initQuickSettingDialog()
        adapter = AppAdapter(::onSelect, ::onUninstall, ::setPackageNameForQuickAccess)
        setupRecView(adapter)
        initView()
        setUpObservers()

    }//end of onCreate

    private fun initView() {
        if (prefs.isDarkTheme) {
            binding.setTheme.setImageDrawable(
                AppCompatResources.getDrawable(requireContext(), R.drawable.ic_lightbulb_filed)
            )
        } else {
            binding.setTheme.setImageDrawable(
                AppCompatResources.getDrawable(requireContext(), R.drawable.ic_lightbulb_empty)
            )
        }

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

        binding.setTheme.setOnClickListener {
            if (!prefs.isDarkTheme) {
                prefs.isDarkTheme = true
                (requireActivity() as OnThemeChangeCallBack).onThemeChanged(true)
//                recreate(requireActivity())
            } else {
                prefs.isDarkTheme = false
//                recreate(requireActivity())
                (requireActivity() as OnThemeChangeCallBack).onThemeChanged(true)
            }
        }
    }

    private fun initQuickSettingDialog() {
        dialogQuickSettingBinding = DialogQuickSettingBinding.inflate(layoutInflater)
        dialog = AlertDialog.Builder(requireContext())
            .setView(dialogQuickSettingBinding.root)
            .create()

        dialogQuickSettingBinding.icClose.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun setUpObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.appListFlow.collect { resource ->
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

//                                Log.e("SubList", "chucked: ${resource.data?.chunked(5)}")
                                Log.i("SubList", "windowed: ${resource.data?.windowed(5,3,partialWindows = true)
                                    ?.get(2)?.size}")

                                adapter.setAppData(resource.data!!)
                                adapter.notifyDataSetChanged()

                            }
                            is Resource.Error -> {
                                binding.pBar.visibility = View.GONE
                                binding.refreshLayout.isRefreshing = false
                                binding.recView.visibility = View.VISIBLE

                                Snackbar.make(binding.root, "Error! Please relaunch the app.", Snackbar.LENGTH_SHORT).show()
                            }
                        }

                    }
                }
            }
        }
    }

    //private functions
    private fun setupRecView(adapter: AppAdapter) {
        binding.recView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.recView.adapter = adapter
    }


    private fun onSelect(packageName: String) {
        val intent = requireActivity().packageManager.getLaunchIntentForPackage(packageName)
        intent?.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        result.launch(intent)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun setPackageNameForQuickAccess(app: AppDetails) {
        val bottomsheetBinding = BottomsheetQuickSettingBinding.inflate(layoutInflater)
        val bottomsheet = BottomSheetDialog(requireContext())
        bottomsheet.setContentView(bottomsheetBinding.root)
        bottomsheet.show()

        bottomsheetBinding.apply {
            titleText.text = app.name
            subQuickAccessText.alpha = 0.6f
            subDemoText.alpha = 0.6f

            appLogo.setImageDrawable(app.image)

            if (prefs.isDarkTheme) {
                logo.setImageResource(R.drawable.ic_android_4)
                demoImage.setImageResource(R.drawable.ic_demo_white)
                icClose.setImageResource(R.drawable.ic_close_white)
            } else {
                logo.setImageResource(R.drawable.ic_android_4)
                demoImage.setImageResource(R.drawable.ic_demo_dark)
                icClose.setImageResource(R.drawable.ic_close_dark)
            }

            icClose.setOnClickListener {
                bottomsheet.dismiss()
            }

            quickAccessLayout.setOnClickListener {
                QuickAccessService().qsTile.icon = Icon.createWithResource(requireContext(),R.drawable.ic_arrow_back)
                QuickAccessService().qsTile.updateTile()
                prefs.quickAccessAppName = app.packageName
                Snackbar.make(
                    binding.root,
                    "Successfully Set App For Quick Setting",
                    Snackbar.LENGTH_SHORT
                ).show()
                bottomsheet.dismiss()
            }

            demo.setOnClickListener {
                dialog.show()
            }
        }
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

    private val result =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == AppCompatActivity.RESULT_OK) {
                Toast.makeText(requireContext(), "Cancelled", Toast.LENGTH_SHORT).show()
            }
        }

    private val unInstallApp =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == AppCompatActivity.RESULT_OK) {
                viewModel.onRefresh()
                Toast.makeText(requireContext(), "Successfully Uninstalled", Toast.LENGTH_SHORT)
                    .show()
            }
        }
}
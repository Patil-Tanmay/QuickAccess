package com.example.quickaccess.ui

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.ViewAnimationUtils
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.quickaccess.MainViewModel
import com.example.quickaccess.R
import com.example.quickaccess.data.AppAdapter
import com.example.quickaccess.data.AppDetails
import com.example.quickaccess.databinding.BottomsheetQuickSettingBinding
import com.example.quickaccess.databinding.DialogQuickSettingBinding
import com.example.quickaccess.databinding.FragmentMainBinding
import com.example.quickaccess.prefs
import com.example.quickaccess.service.QuickAccessService.Companion.isTileAdded
import com.example.quickaccess.utils.*
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream


class MainFragment : Fragment(R.layout.fragment_main) {

    private val binding by viewBinding(FragmentMainBinding::bind)

    private lateinit var adapter: AppAdapter

    private val viewModel by activityViewModels<MainViewModel>()

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
//        appListing = viewModel.getPagedAppList()
        initQuickSettingDialog()
        adapter = AppAdapter(::onSelect, ::onUninstall, ::setPackageNameForQuickAccess)
        setupRecView(adapter)
        initView()
        setUpObservers()

    }//end of onCreate

    private fun initView() {
        hideSystemUi()
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
            viewModel.isSearchViewOpen = true
            binding.searchInputText.requestFocus()
            binding.searchInputText.showKeyBoard()
            openSearch()
        }
        binding.closeSearchButton.setOnClickListener {
            viewModel.isSearchViewOpen = false
            viewModel.setQuery(null)
            this.hideKeyboard()
            closeSearch()
        }

        binding.refreshLayout.setOnRefreshListener {
            binding.refreshLayout.isRefreshing =false
            viewModel.onRefresh()
        }

        binding.setTheme.setOnClickListener {
            if (!prefs.isDarkTheme) {
                prefs.isDarkTheme = true
                (requireActivity() as OnThemeChangeCallBack).onThemeChanged(true)
            } else {
                prefs.isDarkTheme = false
                (requireActivity() as OnThemeChangeCallBack).onThemeChanged(true)
            }
        }

        binding.recView.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
//                if (!recyclerView.canScrollVertically(1)){
//                    viewModel.getNextPagedList()
//                }
                if (dy > 0) {
//                    Log.i("TAGG", "onScrolled: $dy ")
                    val visibleItemCount: Int = recyclerView.layoutManager?.childCount!!
                    val totalItemCount: Int = recyclerView.layoutManager?.itemCount!!
                    val pastVisibleItems: Int =
                        (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                    if (pastVisibleItems + visibleItemCount >= totalItemCount - 6) {
                        if (viewModel.currentQuery != null) {
                            viewModel.filterAppListNextPage()
                        } else {
                            viewModel.getNextPagedList()
                        }
                    }
                }
            }
        })
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

    @SuppressLint("SetTextI18n")
    private fun setUpObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.appListFlow.collect { resource ->
                        when (resource) {
                            is Resource.Loading -> {
                                binding.pBar.visibility = View.VISIBLE
                                binding.refreshLayout.isRefreshing = false
                                binding.recView.visibility = View.GONE
                            }

                            is Resource.Success -> {
                                binding.pBar.visibility = View.GONE
                                binding.refreshLayout.isRefreshing = false
                                binding.recView.visibility = View.VISIBLE

                                adapter.submitList(resource.data!!)
                                if (resource.data.size>20) {
                                    adapter.notifyItemRangeChanged(adapter.currentList.size, resource.data.size)
                                }

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

                launch {
                    viewModel.totalApps.collect{ binding.totalNoOfApps.text = "($it Apps)" }
                }

                launch {
                    viewModel.updateAppListAfterUnInstall.collect{ list ->
//                        val index = adapter.currentList.indexOf(viewModel.currentAppForUnInstall)
                        adapter.submitList(list)
                        adapter.notifyItemRemoved(viewModel.currentAppForUnInstallPosition)
                    }
                }

                launch {
                    viewModel.closeSearchView.collect{
                        if (it){
                            viewModel.isSearchViewOpen = false
                            viewModel.setQuery(null)
                            closeSearch()
                        }
                    }
                }
            }
        }

//        appListing.appList.observe(viewLifecycleOwner){
//            adapter.submitList(it)
//        }
    }

    @SuppressLint("InlinedApi")
    private fun hideSystemUi() {
        WindowCompat.setDecorFitsSystemWindows(requireActivity().window, false)
        WindowInsetsControllerCompat(requireActivity().window, binding.root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.navigationBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun encodeImageDrawable(realImage: Bitmap) {
        val baos = ByteArrayOutputStream()
        realImage.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val b: ByteArray = baos.toByteArray()

        val encodedImage: String = Base64.encodeToString(b, Base64.DEFAULT)

        prefs.imageDrawable = encodedImage
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

            appLogo.setImageBitmap(app.image)

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
                prefs.quickAccessAppName = app.packageName
                if (!isTileAdded) {
                    Snackbar.make(
                        binding.root,
                        "Please add App to the Quick Tile to access this feature.",
                        Snackbar.LENGTH_LONG
                    ).show()
                } else {
                    Snackbar.make(
                        binding.root,
                        "Successfully Set App For Quick Setting",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
                bottomsheet.dismiss()
            }

            demo.setOnClickListener {
                dialog.show()
            }
        }
    }

    private fun onUninstall(app: AppDetails, position: Int) {
        viewModel.setAppForUnInstall(app, position)
        val intent = Intent(Intent.ACTION_DELETE)
        intent.data = Uri.parse("package:${app.packageName}")
        intent.putExtra(Intent.EXTRA_RETURN_RESULT, true)
        unInstallApp.launch(intent)
    }

    private fun openSearch() {
        binding.searchInputText.addTextChangedListener {
            if (!binding.searchInputText.text.isNullOrBlank()) {
//                viewModel.filterAppList(it.toString())
                viewModel.filterAppListPaged(it.toString(), false)
            } else if (binding.searchInputText.text.isBlank()) {
//                viewModel.filterAppList("")
                viewModel.filterAppListPaged("", false)
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
                viewModel.onUnInstall(true)
                Toast.makeText(requireContext(), "Successfully Uninstalled", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.onUnInstall(false)
            }
        }
}
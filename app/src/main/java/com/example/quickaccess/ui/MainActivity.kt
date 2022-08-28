package com.example.quickaccess.ui

import android.animation.Animator
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewAnimationUtils
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.quickaccess.MainApplication
import com.example.quickaccess.MainViewModel
import com.example.quickaccess.R
import com.example.quickaccess.data.AppAdapter
import com.example.quickaccess.databinding.ActivityMainBinding
import com.example.quickaccess.prefs
import com.example.quickaccess.service.QuickAccessService
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.example.quickaccess.utils.Resource
import com.example.quickaccess.utils.hideKeyboard
import com.example.quickaccess.utils.open
import com.example.quickaccess.utils.showKeyBoard


@AndroidEntryPoint
class MainActivity : AppCompatActivity(), MainFragment.OnThemeChangeCallBack {

    private val viewModel by viewModels<MainViewModel>()

    private lateinit var binding: ActivityMainBinding

    private lateinit var adapter: AppAdapter

    override fun onThemeChanged(isChanged: Boolean) {
        supportFragmentManager.open {
            replace(R.id.frag_container,MainFragment())
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        if (!prefs.isDarkTheme) {
//            setTheme(R.style.Theme_QuickAccess)
//        } else {
//            setTheme(R.style.Theme_Dark)
//        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportFragmentManager.open {
            replace(R.id.frag_container,MainFragment())
        }
    }

    companion object {
        private const val MainActivityTag: String = "TAGG"
    }
}

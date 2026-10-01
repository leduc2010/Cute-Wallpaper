package com.cute.wallpaper.ringtones.presentation.main

import android.view.LayoutInflater
import androidx.activity.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.cute.wallpaper.ringtones.R
import com.cute.wallpaper.ringtones.databinding.ActivityMainBinding
import com.cute.wallpaper.ringtones.presentation.base.BaseActivity
import com.cute.wallpaper.ringtones.presentation.navigation.EventObserver
import com.cute.wallpaper.ringtones.presentation.navigation.NavigationViewModel
import com.cute.wallpaper.ringtones.presentation.navigation.safeNavigate
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {

    private lateinit var navController: NavController
    private val navViewModel: NavigationViewModel by viewModels()

    override fun inflateBinding(inflater: LayoutInflater): ActivityMainBinding {
        return ActivityMainBinding.inflate(inflater)
    }

    override fun initView() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController
    }

    override fun observeData() {
        navViewModel.actionDestination.observe(this, EventObserver { action ->
            navController.safeNavigate(
                destination = action.destination,
                args = action.args,
                navOptions = action.navOptions
            )
        })

        navViewModel.naviDirection.observe(this, EventObserver { direction ->
            navController.safeNavigate(direction)
        })

        navViewModel.actionBack.observe(this, EventObserver { action ->
            val popped = action.destinationId?.let { destinationId ->
                navController.popBackStack(destinationId, action.inclusive)
            } ?: navController.popBackStack()

            if (!popped && action.destinationId == null) {
                finish()
            }
        })
    }
}

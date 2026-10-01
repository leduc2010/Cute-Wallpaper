package com.cute.wallpaper.ringtones.presentation.navigation

import android.os.Bundle
import android.util.Log
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import com.cute.wallpaper.ringtones.BuildConfig

private const val TAG = "Navigation"

fun NavController.safeNavigate(
    directions: NavDirections,
    navOptions: NavOptions? = null
) {
    val action = currentDestination?.getAction(directions.actionId)
        ?: graph.getAction(directions.actionId)

    if (action == null) return

    runCatching {
        navigate(directions, navOptions)
    }.onFailure { throwable ->
        if (BuildConfig.DEBUG) {
            Log.w(TAG, "Navigation failed for action=${directions.actionId}", throwable)
        }
    }
}

fun NavController.safeNavigate(
    destination: Int,
    args: Bundle? = null,
    navOptions: NavOptions? = null
) {
    runCatching {
        navigate(destination, args, navOptions)
    }.onFailure { throwable ->
        if (BuildConfig.DEBUG) {
            Log.w(TAG, "Navigation failed for destination=$destination", throwable)
        }
    }
}

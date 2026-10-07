package com.cute.wallpaper.ringtones.utils

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import com.cute.wallpaper.ringtones.R

private const val TOAST_BOTTOM_OFFSET_DP = 222
private const val TOAST_CENTER_OFFSET_DP = -8

fun Context.showSuccessToast(@StringRes messageRes: Int) {
    showDesignToast(messageRes, ToastPlacement.BOTTOM_ACTION)
}

fun Context.showErrorToast(@StringRes messageRes: Int) {
    showDesignToast(messageRes, ToastPlacement.BOTTOM_ACTION)
}

fun Context.showCenterToast(@StringRes messageRes: Int) {
    showDesignToast(messageRes, ToastPlacement.CENTER)
}

@Suppress("DEPRECATION")
private fun Context.showDesignToast(
    @StringRes messageRes: Int,
    placement: ToastPlacement
) {
    val view = LayoutInflater.from(this)
        .inflate(R.layout.custom_toast_message, null, false)
    view.findViewById<TextView>(R.id.tvToastMessage).setText(messageRes)

    Toast(applicationContext).apply {
        duration = Toast.LENGTH_SHORT
        this.view = view
        when (placement) {
            ToastPlacement.BOTTOM_ACTION -> setGravity(
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
                0,
                dpToPx(TOAST_BOTTOM_OFFSET_DP)
            )
            ToastPlacement.CENTER -> setGravity(
                Gravity.CENTER,
                0,
                dpToPx(TOAST_CENTER_OFFSET_DP)
            )
        }
    }.show()
}

private enum class ToastPlacement {
    BOTTOM_ACTION,
    CENTER
}

private fun Context.dpToPx(value: Int): Int =
    (value * resources.displayMetrics.density).toInt()

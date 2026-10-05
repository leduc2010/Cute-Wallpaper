package com.cute.wallpaper.ringtones.presentation.base

import android.app.Dialog
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.viewbinding.ViewBinding

abstract class BaseDialogFragment<VB : ViewBinding> : DialogFragment() {
    protected open val gravity: Int = Gravity.CENTER
    protected open val canceledOnTouchOutside: Boolean = true
    protected open val cancelBackPress: Boolean = true
    protected open val fullScreen: Boolean = false
    protected open val marginInDp: Int = 0
    protected open val widthMatchParent: Boolean = false

    private var _binding: VB? = null
    protected val binding: VB
        get() = requireNotNull(_binding)

    protected abstract fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): VB

    protected abstract fun initView()

    protected open fun initListener() = Unit

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireContext(), theme).apply {
            setCanceledOnTouchOutside(canceledOnTouchOutside)
            setOnKeyListener { _, keyCode, event ->
                !cancelBackPress &&
                    keyCode == KeyEvent.KEYCODE_BACK &&
                    event.action == KeyEvent.ACTION_UP
            }
            window?.apply {
                setGravity(gravity)
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = inflateBinding(inflater, container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initListener()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            val marginInPx = (marginInDp * resources.displayMetrics.density).toInt()
            val screenWidth = Resources.getSystem().displayMetrics.widthPixels
            val dialogWidth = screenWidth - marginInPx * 2
            setLayout(
                if (widthMatchParent) {
                    WindowManager.LayoutParams.MATCH_PARENT
                } else if (marginInPx > 0) {
                    dialogWidth
                } else {
                    WindowManager.LayoutParams.WRAP_CONTENT
                },
                if (fullScreen) {
                    WindowManager.LayoutParams.MATCH_PARENT
                } else {
                    WindowManager.LayoutParams.WRAP_CONTENT
                }
            )
            hideNavigationBar(decorView)
        }
    }

    override fun show(manager: FragmentManager, tag: String?) {
        if (manager.isDestroyed || manager.isStateSaved) return
        super.show(manager, tag)
    }

    protected fun safeDismiss() {
        if (dialog?.isShowing == true && isAdded) {
            dismiss()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun hideNavigationBar(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.windowInsetsController?.apply {
                hide(WindowInsets.Type.navigationBars())
                systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            view.systemUiVisibility =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }
    }
}

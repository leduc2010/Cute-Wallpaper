package com.cute.wallpaper.ringtones.presentation.autochangewallpaper

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import com.cute.wallpaper.ringtones.R
import com.cute.wallpaper.ringtones.databinding.DialogAutoChangeMinimumBinding
import com.cute.wallpaper.ringtones.domain.model.AutoChangeConfig
import com.cute.wallpaper.ringtones.presentation.base.BaseDialogFragment

class AutoChangeMinimumDialogFragment :
    BaseDialogFragment<DialogAutoChangeMinimumBinding>() {

    override val marginInDp = 40

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = DialogAutoChangeMinimumBinding.inflate(inflater, container, false)

    override fun initView() {
        binding.tvMessage.text = getString(
            R.string.auto_change_minimum_dialog_message,
            AutoChangeConfig.MIN_DOWNLOADED_WALLPAPERS
        )
    }

    override fun initListener() {
        binding.btnClose.setOnClickListener { safeDismiss() }
        binding.btnOk.setOnClickListener { safeDismiss() }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.6f)
        }
    }

    companion object {
        const val TAG = "AutoChangeMinimumDialog"
    }
}

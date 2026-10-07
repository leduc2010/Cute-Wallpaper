package com.cute.wallpaper.ringtones.presentation.autochangewallpaper

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import kotlin.math.roundToInt
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.cute.wallpaper.ringtones.R
import com.cute.wallpaper.ringtones.databinding.DialogAutoChangeWallpaperBinding
import com.cute.wallpaper.ringtones.domain.model.AutoChangeConfig
import com.cute.wallpaper.ringtones.domain.model.WallpaperTarget
import com.cute.wallpaper.ringtones.presentation.base.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AutoChangeWallpaperDialogFragment :
    BaseDialogFragment<DialogAutoChangeWallpaperBinding>() {

    override val marginInDp = 20

    private val viewModel: AutoChangeWallpaperViewModel by viewModels()

    private var downloadedCount = 0
    private lateinit var savedTarget: WallpaperTarget
    private var savedIntervalHours = AutoChangeConfig.DEFAULT_INTERVAL_HOURS
    private var enabled = false

    private var selectedTarget = WallpaperTarget.BOTH
    private var selectedIntervalHours = AutoChangeConfig.DEFAULT_INTERVAL_HOURS

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = DialogAutoChangeWallpaperBinding.inflate(inflater, container, false)

    override fun initView() {
        val config = viewModel.currentConfig()
        enabled = config.enabled
        savedTarget = config.target
        savedIntervalHours = config.intervalHours
        selectedTarget = savedTarget
        selectedIntervalHours = savedIntervalHours

        viewModel.downloadedWallpapers.observe(viewLifecycleOwner) { wallpapers ->
            downloadedCount = wallpapers.size
            render()
        }
        render()
    }

    override fun initListener() {
        binding.btnClose.setOnClickListener { safeDismiss() }

        binding.optionHome.setOnClickListener { selectTarget(WallpaperTarget.HOME) }
        binding.optionLock.setOnClickListener { selectTarget(WallpaperTarget.LOCK) }
        binding.optionBoth.setOnClickListener { selectTarget(WallpaperTarget.BOTH) }

        binding.option3Hours.setOnClickListener { selectInterval(3) }
        binding.option6Hours.setOnClickListener { selectInterval(6) }
        binding.option12Hours.setOnClickListener { selectInterval(12) }
        binding.option24Hours.setOnClickListener { selectInterval(24) }

        binding.btnEnable.setOnClickListener {
            if (!ensureEnoughWallpapers()) return@setOnClickListener
            viewModel.enableOrSave(selectedTarget, selectedIntervalHours)
            safeDismiss()
        }
        binding.btnSaveChanges.setOnClickListener {
            if (!ensureEnoughWallpapers()) return@setOnClickListener
            viewModel.enableOrSave(selectedTarget, selectedIntervalHours)
            safeDismiss()
        }
        binding.btnStop.setOnClickListener {
            viewModel.stop()
            safeDismiss()
        }
    }

    private fun selectTarget(target: WallpaperTarget) {
        if (!ensureEnoughWallpapers()) return
        selectedTarget = target
        render()
    }

    private fun selectInterval(hours: Int) {
        if (!ensureEnoughWallpapers()) return
        selectedIntervalHours = hours
        render()
    }

    private fun ensureEnoughWallpapers(): Boolean {
        if (downloadedCount >= AutoChangeConfig.MIN_DOWNLOADED_WALLPAPERS) return true

        if (
            childFragmentManager.findFragmentByTag(
                AutoChangeMinimumDialogFragment.TAG
            ) == null
        ) {
            AutoChangeMinimumDialogFragment().show(
                childFragmentManager,
                AutoChangeMinimumDialogFragment.TAG
            )
        }
        return false
    }

    private fun render() {
        binding.downloadedCount.text = getString(
            R.string.auto_change_downloaded_count,
            downloadedCount
        )

        val hasEnoughWallpapers = downloadedCount >= AutoChangeConfig.MIN_DOWNLOADED_WALLPAPERS
        binding.minimumBanner.isVisible = !hasEnoughWallpapers
        binding.minimumHint.text = getString(
            R.string.auto_change_minimum_hint,
            AutoChangeConfig.MIN_DOWNLOADED_WALLPAPERS,
            downloadedCount,
            AutoChangeConfig.MIN_DOWNLOADED_WALLPAPERS
        )

        binding.optionHome.isSelected = selectedTarget == WallpaperTarget.HOME
        binding.optionLock.isSelected = selectedTarget == WallpaperTarget.LOCK
        binding.optionBoth.isSelected = selectedTarget == WallpaperTarget.BOTH

        renderFrequencyOption(binding.option3Hours, selectedIntervalHours == 3)
        renderFrequencyOption(binding.option6Hours, selectedIntervalHours == 6)
        renderFrequencyOption(binding.option12Hours, selectedIntervalHours == 12)
        renderFrequencyOption(binding.option24Hours, selectedIntervalHours == 24)

        val changed = selectedTarget != savedTarget ||
            selectedIntervalHours != savedIntervalHours

        binding.btnEnable.isVisible = !enabled
        binding.btnSaveChanges.isVisible = enabled && changed
        binding.btnStop.isVisible = enabled
        if (enabled) {
            val topMarginDp = if (changed) 12 else 32
            val params = binding.btnStop.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = dp(topMarginDp)
            binding.btnStop.layoutParams = params
        }
    }

    private fun renderFrequencyOption(view: View, selected: Boolean) {
        view.isSelected = selected
        val params = view.layoutParams
        val targetHeight = dp(if (selected) 44 else 40)
        if (params.height != targetHeight) {
            params.height = targetHeight
            view.layoutParams = params
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).roundToInt()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.7f)
        }
    }

    companion object {
        const val TAG = "AutoChangeWallpaperDialog"
    }
}

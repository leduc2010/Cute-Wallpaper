package com.cute.wallpaper.ringtones.presentation.autochangewallpaper

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.cute.wallpaper.ringtones.R
import com.cute.wallpaper.ringtones.databinding.DialogAutoChangeWallpaperBinding
import com.cute.wallpaper.ringtones.domain.model.AutoChangeConfig
import com.cute.wallpaper.ringtones.domain.model.WallpaperTarget
import com.cute.wallpaper.ringtones.presentation.base.BaseDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(
                getString(
                    R.string.auto_change_minimum_dialog_message,
                    AutoChangeConfig.MIN_DOWNLOADED_WALLPAPERS
                )
            )
            .setPositiveButton(R.string.ok, null)
            .show()
        return false
    }

    private fun render() {
        binding.downloadedCount.text = getString(
            R.string.auto_change_downloaded_count,
            downloadedCount
        )

        val hasEnoughWallpapers = downloadedCount >= AutoChangeConfig.MIN_DOWNLOADED_WALLPAPERS
        binding.minimumHint.isVisible = !hasEnoughWallpapers
        binding.minimumHint.text = getString(
            R.string.auto_change_minimum_hint,
            AutoChangeConfig.MIN_DOWNLOADED_WALLPAPERS,
            downloadedCount,
            AutoChangeConfig.MIN_DOWNLOADED_WALLPAPERS
        )

        binding.optionHome.isSelected = selectedTarget == WallpaperTarget.HOME
        binding.optionLock.isSelected = selectedTarget == WallpaperTarget.LOCK
        binding.optionBoth.isSelected = selectedTarget == WallpaperTarget.BOTH

        binding.option3Hours.isSelected = selectedIntervalHours == 3
        binding.option6Hours.isSelected = selectedIntervalHours == 6
        binding.option12Hours.isSelected = selectedIntervalHours == 12
        binding.option24Hours.isSelected = selectedIntervalHours == 24

        val changed = selectedTarget != savedTarget ||
            selectedIntervalHours != savedIntervalHours

        binding.btnEnable.isVisible = !enabled
        binding.btnSaveChanges.isVisible = enabled && changed
        binding.btnStop.isVisible = enabled
    }

    companion object {
        const val TAG = "AutoChangeWallpaperDialog"
    }
}

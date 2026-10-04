package com.cute.wallpaper.ringtones.presentation.detail

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.cute.wallpaper.ringtones.databinding.BottomSheetWallpaperTargetBinding
import com.cute.wallpaper.ringtones.domain.model.WallpaperTarget
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class WallpaperTargetBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetWallpaperTargetBinding? = null
    private val binding: BottomSheetWallpaperTargetBinding
        get() = requireNotNull(_binding)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return BottomSheetWallpaperTargetBinding.inflate(inflater, container, false)
            .also { _binding = it }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnClose.setOnClickListener { dismiss() }
        binding.btnHome.setOnClickListener { selectTarget(WallpaperTarget.HOME) }
        binding.btnLock.setOnClickListener { selectTarget(WallpaperTarget.LOCK) }
        binding.btnBoth.setOnClickListener { selectTarget(WallpaperTarget.BOTH) }
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.findViewById<FrameLayout>(
            com.google.android.material.R.id.design_bottom_sheet
        )?.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun selectTarget(target: WallpaperTarget) {
        parentFragmentManager.setFragmentResult(
            REQUEST_KEY,
            Bundle().apply { putString(RESULT_TARGET, target.name) }
        )
        dismiss()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "WallpaperTargetBottomSheet"
        const val REQUEST_KEY = "wallpaper_target_request"
        const val RESULT_TARGET = "wallpaper_target"
    }
}

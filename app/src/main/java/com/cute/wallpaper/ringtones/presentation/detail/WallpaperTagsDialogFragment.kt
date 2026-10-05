package com.cute.wallpaper.ringtones.presentation.detail

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.cute.wallpaper.ringtones.R
import com.cute.wallpaper.ringtones.databinding.DialogWallpaperTagsBinding
import com.cute.wallpaper.ringtones.presentation.base.BaseDialogFragment
import com.google.android.material.chip.Chip

class WallpaperTagsDialogFragment : BaseDialogFragment<DialogWallpaperTagsBinding>() {
    private var selectionDispatched = false
    override val gravity: Int = Gravity.CENTER
    override val canceledOnTouchOutside: Boolean = true
    override val cancelBackPress: Boolean = true
    override val fullScreen: Boolean = false
    override val marginInDp: Int = 20

    private val tags: List<String>
        get() = arguments?.getStringArrayList(ARG_TAGS).orEmpty()

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): DialogWallpaperTagsBinding {
        return DialogWallpaperTagsBinding.inflate(inflater, container, false)
    }

    override fun initView() {
        selectionDispatched = false
        renderTags()
    }

    override fun initListener() {
        binding.btnCloseIcon.setOnClickListener { safeDismiss() }
        binding.btnClose.setOnClickListener { safeDismiss() }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            attributes = attributes.apply {
                dimAmount = DIALOG_DIM_AMOUNT
            }
        }
    }

    private fun renderTags() {
        val chipIds = tags.map { tag ->
            val chip = layoutInflater.inflate(
                R.layout.item_wallpaper_tag_chip,
                binding.tagsContainer,
                false
            ) as Chip
            chip.id = View.generateViewId()
            chip.text = tag
            chip.setOnClickListener {
                if (selectionDispatched || parentFragmentManager.isStateSaved) return@setOnClickListener
                selectionDispatched = true
                parentFragmentManager.setFragmentResult(
                    REQUEST_KEY,
                    Bundle().apply { putString(RESULT_TAG, tag) }
                )
                safeDismiss()
            }
            binding.tagsContainer.addView(chip)
            chip.id
        }
        binding.tagsFlow.referencedIds = chipIds.toIntArray()
    }

    companion object {
        const val TAG = "WallpaperTagsDialog"
        const val REQUEST_KEY = "wallpaper_tag_selected"
        const val RESULT_TAG = "wallpaper_tag"

        private const val ARG_TAGS = "wallpaper_tags"
        private const val DIALOG_DIM_AMOUNT = 0.62f

        fun newInstance(tags: Collection<String>) = WallpaperTagsDialogFragment().apply {
            arguments = Bundle().apply {
                putStringArrayList(ARG_TAGS, ArrayList(tags))
            }
        }
    }
}

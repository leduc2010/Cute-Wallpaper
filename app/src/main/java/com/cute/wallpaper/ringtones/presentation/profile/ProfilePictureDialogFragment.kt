package com.cute.wallpaper.ringtones.presentation.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.cute.wallpaper.ringtones.R
import com.cute.wallpaper.ringtones.databinding.DialogProfilePictureBinding
import com.cute.wallpaper.ringtones.presentation.base.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfilePictureDialogFragment : BaseDialogFragment<DialogProfilePictureBinding>() {
    override val marginInDp = 20
    private val viewModel: ProfilePictureDetailViewModel by viewModels(ownerProducer = { requireParentFragment() })
    private val photo get() = arguments?.getBoolean("photo") == true

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = DialogProfilePictureBinding.inflate(inflater, container, false)

    override fun initView() {
        binding.root.setBackgroundResource(
            if (photo) R.drawable.profile_editor_dialog_background
            else R.drawable.profile_editor_download_background
        )
        binding.privacyIcon.isVisible = photo
        binding.downloadPreview.isVisible = !photo
        binding.title.isVisible = !photo
        binding.title.text = viewModel.currentItem?.title.orEmpty()
        binding.description.setText(
            if (photo) R.string.profile_editor_privacy else R.string.profile_editor_save_description
        )
        binding.confirm.setText(
            if (photo) R.string.profile_editor_choose_photo else R.string.profile_editor_download_image
        )
        if (!photo) {
            viewModel.bitmap.observe(viewLifecycleOwner) { renderDownload() }
            viewModel.name.observe(viewLifecycleOwner) { renderDownload() }
            viewModel.adjustment.observe(viewLifecycleOwner) { renderDownload() }
            viewModel.frame.observe(viewLifecycleOwner) { renderDownload() }
            viewModel.loading.observe(viewLifecycleOwner) { renderDownload() }
            viewModel.saving.observe(viewLifecycleOwner) { renderDownload() }
        }
    }

    private fun renderDownload() {
        binding.downloadPreview.setPhotoBitmap(viewModel.bitmap.value)
        val bg = ContextCompat.getColor(requireContext(), R.color.profile_editor_preview_background)
        binding.downloadPreview.renderState(
            viewModel.transform,
            viewModel.name.value.orEmpty(),
            ProfileAdjustment.valueOf(viewModel.adjustment.value ?: "ORIGINAL"),
            ProfileFrame.valueOf(viewModel.frame.value ?: "NONE"),
            bg
        )
        val busy = viewModel.saving.value == true || viewModel.loading.value == true
        binding.confirm.isEnabled = !busy && viewModel.bitmap.value != null && viewModel.currentItem?.downloadEnabled == true
        binding.confirm.alpha = if (binding.confirm.isEnabled) 1f else .5f
        binding.saveProgress.isVisible = busy
    }

    override fun initListener() {
        binding.close.setOnClickListener { safeDismiss() }
        binding.confirm.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                REQUEST,
                Bundle().apply { putBoolean("photo", photo) }
            )
            if (photo) safeDismiss()
        }
    }

    override fun onDestroyView() {
        binding.downloadPreview.setPhotoBitmap(null)
        super.onDestroyView()
    }

    companion object {
        const val REQUEST = "profile_dialog_action"
        const val PRIVACY = "profile_privacy"
        const val DOWNLOAD = "profile_download"
        fun newInstance(photo: Boolean) = ProfilePictureDialogFragment().apply {
            arguments = Bundle().apply { putBoolean("photo", photo) }
        }
    }
}

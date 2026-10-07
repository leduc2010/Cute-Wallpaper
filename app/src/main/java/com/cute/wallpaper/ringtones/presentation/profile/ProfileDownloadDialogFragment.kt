package com.cute.wallpaper.ringtones.presentation.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.cute.wallpaper.ringtones.R
import com.cute.wallpaper.ringtones.databinding.DialogProfileDownloadBinding
import com.cute.wallpaper.ringtones.presentation.base.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileDownloadDialogFragment : BaseDialogFragment<DialogProfileDownloadBinding>() {

    override val marginInDp = 29

    private val viewModel: ProfilePictureDetailViewModel by viewModels(
        ownerProducer = { requireParentFragment() }
    )

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = DialogProfileDownloadBinding.inflate(inflater, container, false)

    override fun initView() {
        viewModel.bitmap.observe(viewLifecycleOwner) { render() }
        viewModel.name.observe(viewLifecycleOwner) { render() }
        viewModel.adjustment.observe(viewLifecycleOwner) { render() }
        viewModel.frame.observe(viewLifecycleOwner) { render() }
        viewModel.loading.observe(viewLifecycleOwner) { render() }
        viewModel.saving.observe(viewLifecycleOwner) { render() }
    }

    override fun initListener() {
        binding.close.setOnClickListener { safeDismiss() }
        binding.downloadAction.setOnClickListener {
            parentFragmentManager.setFragmentResult(REQUEST, android.os.Bundle.EMPTY)
        }
    }

    private fun render() {
        binding.preview.setPhotoBitmap(viewModel.bitmap.value)
        binding.preview.renderState(
            viewModel.transform,
            viewModel.name.value.orEmpty(),
            ProfileAdjustment.valueOf(viewModel.adjustment.value ?: "ORIGINAL"),
            ProfileFrame.valueOf(viewModel.frame.value ?: "NONE"),
            ContextCompat.getColor(requireContext(), R.color.profile_editor_preview_background)
        )

        val busy = viewModel.saving.value == true || viewModel.loading.value == true
        binding.downloadAction.isEnabled =
            !busy && viewModel.bitmap.value != null && viewModel.currentItem?.downloadEnabled == true
        binding.downloadLabel.isVisible = !busy
        binding.progress.isVisible = busy
    }

    override fun onDestroyView() {
        binding.preview.setPhotoBitmap(null)
        super.onDestroyView()
    }

    companion object {
        const val REQUEST = "profile_download_action"
        const val TAG = "profile_download"
    }
}

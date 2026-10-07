package com.cute.wallpaper.ringtones.presentation.profile

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import com.cute.wallpaper.ringtones.R
import com.cute.wallpaper.ringtones.databinding.FragmentProfilePictureDetailBinding
import com.cute.wallpaper.ringtones.databinding.ItemProfileEditorPresetBinding
import com.cute.wallpaper.ringtones.presentation.base.BaseFragment
import com.cute.wallpaper.ringtones.utils.showErrorToast
import com.cute.wallpaper.ringtones.utils.showSuccessToast
import com.leansoft.ads.AdManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfilePictureDetailFragment : BaseFragment<FragmentProfilePictureDetailBinding>() {
    private val viewModel: ProfilePictureDetailViewModel by viewModels()
    private val picker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        restoreResumeAds()
        if (uri != null) {
            // Photo Picker/SAF both support this when the provider grants persistence.
            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Loading reports inaccessible/revoked input.
            }
            viewModel.selectPhoto(uri.toString())
        }
    }
    private val storagePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) savePicture()
        else requireContext().showErrorToast(R.string.profile_editor_permission_denied)
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentProfilePictureDetailBinding.inflate(inflater, container, false)

    override fun initView() {
        binding.photoPreview.drawSparkles = false
        childFragmentManager.setFragmentResultListener(
            ProfilePictureDialogFragment.REQUEST,
            viewLifecycleOwner
        ) { _, _ ->
            openPicker()
        }
        childFragmentManager.setFragmentResultListener(
            ProfileDownloadDialogFragment.REQUEST,
            viewLifecycleOwner
        ) { _, _ ->
            requestSave()
        }
        binding.photoPreview.onTransformChanged = viewModel::updateTransform
    }

    override fun initListener() {
        binding.btnBack.setOnClickListener {
            hideKeyboard()
            navViewModel.back()
        }
        binding.btnPrevious.setOnClickListener {
            hideKeyboard()
            viewModel.select(-1)
        }
        binding.btnNext.setOnClickListener {
            hideKeyboard()
            viewModel.select(1)
        }
        binding.btnPhoto.setOnClickListener {
            hideKeyboard()
            showPrivacyDialog()
        }
        binding.btnText.setOnClickListener { viewModel.tool.value = "TEXT" }
        binding.btnDownload.setOnClickListener {
            hideKeyboard()
            showDownloadDialog()
        }
        binding.btnAdjust.setOnClickListener {
            hideKeyboard()
            viewModel.tool.value = "ADJUST"
        }
        binding.btnFrame.setOnClickListener {
            hideKeyboard()
            viewModel.tool.value = "FRAME"
        }
        binding.nameInput.doAfterTextChanged { viewModel.name.value = it?.toString().orEmpty() }
        binding.nameInput.setOnEditorActionListener { _, _, _ ->
            hideKeyboard()
            viewModel.tool.value = ""
            true
        }
        binding.loadError.setOnClickListener { viewModel.reloadPhoto() }
    }

    override fun observeData() {
        viewModel.position.observe(viewLifecycleOwner) { render() }
        viewModel.bitmap.observe(viewLifecycleOwner) {
            binding.photoPreview.setPhotoBitmap(it)
            render()
        }
        viewModel.photoUri.observe(viewLifecycleOwner) { render() }
        viewModel.name.observe(viewLifecycleOwner) { name ->
            if (binding.nameInput.text.toString() != name) binding.nameInput.setText(name)
            render()
        }
        viewModel.adjustment.observe(viewLifecycleOwner) { render() }
        viewModel.frame.observe(viewLifecycleOwner) { render() }
        viewModel.tool.observe(viewLifecycleOwner) { tool ->
            render()
            if (tool == "TEXT") {
                binding.nameInput.requestFocus()
                androidx.core.view.WindowCompat.getInsetsController(
                    requireActivity().window,
                    binding.nameInput
                ).show(WindowInsetsCompat.Type.ime())
            }
        }
        viewModel.loading.observe(viewLifecycleOwner) { render() }
        viewModel.loadFailed.observe(viewLifecycleOwner) { render() }
        viewModel.saving.observe(viewLifecycleOwner) { render() }
        viewModel.result.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { success ->
                if (success) {
                    (childFragmentManager.findFragmentByTag(ProfileDownloadDialogFragment.TAG)
                        as? ProfileDownloadDialogFragment)?.dismiss()
                }
                if (success) {
                    requireContext().showSuccessToast(R.string.profile_editor_saved)
                } else {
                    requireContext().showErrorToast(R.string.profile_editor_save_failed)
                }
            }
        }
    }

    private fun render() {
        val tool = viewModel.tool.value
        val busy = viewModel.saving.value == true
        val ready = viewModel.bitmap.value != null && !busy
        val adjustment = ProfileAdjustment.valueOf(viewModel.adjustment.value ?: "ORIGINAL")
        val frame = ProfileFrame.valueOf(viewModel.frame.value ?: "NONE")
        binding.loading.isVisible = viewModel.loading.value == true
        binding.loadError.isVisible = viewModel.loadFailed.value == true
        binding.nameInput.isVisible = tool == "TEXT" && ready
        binding.photoPreview.photoGesturesEnabled = viewModel.photoUri.value.orEmpty().isNotEmpty() && ready
        binding.photoPreview.renderState(
            viewModel.transform,
            if (tool == "TEXT") "" else viewModel.name.value.orEmpty(),
            adjustment,
            frame,
            backgroundColor()
        )
        binding.btnPrevious.isVisible = (viewModel.position.value ?: 0) > 0
        binding.btnNext.isVisible = (viewModel.position.value ?: 0) < viewModel.items.lastIndex
        binding.btnPrevious.isEnabled = !busy
        binding.btnNext.isEnabled = !busy
        binding.btnPhoto.isEnabled = !busy && viewModel.currentItem != null
        binding.btnText.isEnabled = ready
        binding.btnDownload.isVisible = true
        binding.btnDownload.isEnabled = ready && viewModel.currentItem?.downloadEnabled == true
        binding.btnAdjust.isEnabled = ready
        binding.btnFrame.isEnabled = ready
        val buttons = listOf(binding.btnPhoto, binding.btnText, binding.btnDownload, binding.btnAdjust, binding.btnFrame)
        val tools = listOf("PHOTO", "TEXT", "DOWNLOAD", "ADJUST", "FRAME")
        buttons.forEachIndexed { i, button ->
            button.isSelected = tool == tools[i]
        }
        binding.positionHint.isVisible = tool == "PHOTO" && viewModel.photoUri.value.orEmpty().isNotEmpty() && ready
        binding.presetScroll.isVisible = tool == "ADJUST" || tool == "FRAME" || tool == "TEXT"
        renderPresets(adjustment, frame)
    }

    private fun renderPresets(adjustment: ProfileAdjustment, frame: ProfileFrame) {
        binding.presets.removeAllViews()
        val tool = viewModel.tool.value
        if (tool != "ADJUST" && tool != "FRAME" && tool != "TEXT") return
        val isFrame = tool != "ADJUST"
        val framePresets = listOf(ProfileFrame.NONE, ProfileFrame.PINK, ProfileFrame.PURPLE)
        // Older editor state used SPARKLE as a preset; it is now the fixed Figma decoration.
        val selectedFrame = if (frame == ProfileFrame.SPARKLE) ProfileFrame.NONE else frame
        val labels = if (isFrame) {
            listOf(
                R.string.profile_editor_none,
                R.string.profile_editor_pink,
                R.string.profile_editor_purple
            )
        } else {
            listOf(
                R.string.profile_editor_original,
                R.string.profile_editor_mono,
                R.string.profile_editor_warm,
                R.string.profile_editor_cool
            )
        }
        labels.forEachIndexed { index, label ->
            val selected = if (isFrame) selectedFrame == framePresets[index] else adjustment.ordinal == index
            val preset = ItemProfileEditorPresetBinding.inflate(
                layoutInflater,
                binding.presets,
                false
            ).apply {
                root.contentDescription = getString(label)
                root.isSelected = selected
                root.setOnClickListener {
                    if (isFrame) viewModel.frame.value = framePresets[index].name
                    else viewModel.adjustment.value = ProfileAdjustment.entries[index].name
                }
            }
            if (isFrame && index == 0) {
                preset.noneIcon.isVisible = true
            } else {
                preset.preview.isVisible = true
                preset.preview.thumbnailCornerRadius = dp(if (isFrame) 12 else 28).toFloat()
                preset.preview.setPhotoBitmap(viewModel.bitmap.value)
                preset.preview.renderState(
                    viewModel.transform,
                    "",
                    if (isFrame) adjustment else ProfileAdjustment.entries[index],
                    if (isFrame) framePresets[index] else frame,
                    backgroundColor()
                )
            }
            binding.presets.addView(preset.root)
        }
    }

    private fun backgroundColor() = ContextCompat.getColor(requireContext(), R.color.profile_editor_preview_background)

    private fun showPrivacyDialog() {
        if (childFragmentManager.isStateSaved) return
        if (childFragmentManager.findFragmentByTag(ProfilePictureDialogFragment.TAG) == null) {
            ProfilePictureDialogFragment().show(
                childFragmentManager,
                ProfilePictureDialogFragment.TAG
            )
        }
    }

    private fun showDownloadDialog() {
        if (childFragmentManager.isStateSaved) return
        if (childFragmentManager.findFragmentByTag(ProfileDownloadDialogFragment.TAG) == null) {
            ProfileDownloadDialogFragment().show(
                childFragmentManager,
                ProfileDownloadDialogFragment.TAG
            )
        }
    }

    private fun openPicker() {
        if (viewModel.resumeAdsBeforePicker != null) return
        viewModel.resumeAdsBeforePicker = AdManager.instance.canShowAdResume
        AdManager.instance.canShowAdResume = false
        try {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (_: Exception) {
            restoreResumeAds()
            requireContext().showErrorToast(R.string.profile_editor_picker_failed)
        }
    }

    private fun restoreResumeAds() {
        viewModel.resumeAdsBeforePicker?.let { AdManager.instance.canShowAdResume = it }
        viewModel.resumeAdsBeforePicker = null
    }

    private fun requestSave() {
        if (Build.VERSION.SDK_INT <= 28 && ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else savePicture()
    }

    private fun savePicture() {
        val snapshot = binding.photoPreview.snapshot() ?: return
        viewModel.download(snapshot.copy(name = viewModel.name.value.orEmpty()))
    }

    private fun hideKeyboard() {
        binding.nameInput.clearFocus()
        (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        binding.photoPreview.onTransformChanged = null
        binding.photoPreview.setPhotoBitmap(null)
        binding.presets.removeAllViews()
        super.onDestroyView()
    }

    override fun onDestroy() {
        if (isRemoving || requireActivity().isFinishing) restoreResumeAds()
        super.onDestroy()
    }
}

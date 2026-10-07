package com.cute.wallpaper.ringtones.presentation.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.cute.wallpaper.ringtones.databinding.DialogProfilePictureBinding
import com.cute.wallpaper.ringtones.presentation.base.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfilePictureDialogFragment : BaseDialogFragment<DialogProfilePictureBinding>() {

    override val marginInDp = 20

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = DialogProfilePictureBinding.inflate(inflater, container, false)

    override fun initView() = Unit

    override fun initListener() {
        binding.close.setOnClickListener { safeDismiss() }
        binding.confirm.setOnClickListener {
            parentFragmentManager.setFragmentResult(REQUEST, Bundle.EMPTY)
            safeDismiss()
        }
    }

    companion object {
        const val REQUEST = "profile_photo_privacy_action"
        const val TAG = "profile_privacy"
    }
}

package com.cute.wallpaper.ringtones.presentation.ringtone

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.cute.wallpaper.ringtones.databinding.BottomSheetRingtoneTargetBinding
import com.cute.wallpaper.ringtones.domain.model.RingtoneTarget
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class RingtoneTargetBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetRingtoneTargetBinding? = null
    private val binding: BottomSheetRingtoneTargetBinding
        get() = requireNotNull(_binding)

    private val contentId: String
        get() = requireArguments().getString(ARG_CONTENT_ID).orEmpty()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return BottomSheetRingtoneTargetBinding.inflate(inflater, container, false)
            .also { _binding = it }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnClose.setOnClickListener { dismiss() }
        binding.btnRingtone.setOnClickListener { selectTarget(RingtoneTarget.RINGTONE) }
        binding.btnNotification.setOnClickListener { selectTarget(RingtoneTarget.NOTIFICATION) }
        binding.btnAlarm.setOnClickListener { selectTarget(RingtoneTarget.ALARM) }
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.findViewById<FrameLayout>(
            com.google.android.material.R.id.design_bottom_sheet
        )?.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun selectTarget(target: RingtoneTarget) {
        parentFragmentManager.setFragmentResult(
            REQUEST_KEY,
            Bundle().apply {
                putString(RESULT_CONTENT_ID, contentId)
                putString(RESULT_TARGET, target.name)
            }
        )
        dismiss()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "RingtoneTargetBottomSheet"
        const val REQUEST_KEY = "ringtone_target_request"
        const val RESULT_CONTENT_ID = "ringtone_content_id"
        const val RESULT_TARGET = "ringtone_target"
        private const val ARG_CONTENT_ID = "ringtone_target_content_id"

        fun newInstance(contentId: String) = RingtoneTargetBottomSheet().apply {
            arguments = Bundle().apply { putString(ARG_CONTENT_ID, contentId) }
        }
    }
}

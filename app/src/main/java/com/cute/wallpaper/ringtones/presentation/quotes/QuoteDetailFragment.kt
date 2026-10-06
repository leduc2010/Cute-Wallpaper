package com.cute.wallpaper.ringtones.presentation.quotes

import android.Manifest
import android.content.Intent
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import com.cute.wallpaper.ringtones.R
import com.cute.wallpaper.ringtones.databinding.FragmentQuoteDetailBinding
import com.cute.wallpaper.ringtones.presentation.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class QuoteDetailFragment : BaseFragment<FragmentQuoteDetailBinding>() {
    private val viewModel: QuoteDetailViewModel by viewModels()
    private val storagePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) savePoster() else toast(R.string.quotes_permission_denied)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentQuoteDetailBinding.inflate(inflater, container, false)

    override fun initView() {
        binding.poster.setQuote(viewModel.item?.quote.orEmpty())
    }

    override fun initListener() {
        binding.btnClose.setOnClickListener { navViewModel.back() }
        binding.btnDownload.setOnClickListener {
            if (Build.VERSION.SDK_INT <= 28 && ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else savePoster()
        }
        binding.btnShare.setOnClickListener {
            binding.poster.snapshot()?.let(viewModel::share)
        }
    }

    override fun observeData() {
        viewModel.exporting.observe(viewLifecycleOwner) { busy ->
            binding.exportProgress.isVisible = busy
            val ready = !viewModel.item?.quote.isNullOrBlank() && !busy
            binding.btnDownload.isEnabled = ready
            binding.btnShare.isEnabled = ready
            binding.btnDownload.alpha = if (binding.btnDownload.isEnabled) 1f else .4f
            binding.btnShare.alpha = if (ready) 1f else .4f
        }
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (val action = event.getContentIfNotHandled() ?: return@observe) {
                QuoteExportEvent.Saved -> toast(R.string.quotes_saved)
                QuoteExportEvent.Failed -> toast(R.string.quotes_export_failed)
                is QuoteExportEvent.ShareReady -> {
                    try {
                        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, action.uri)
                            clipData = android.content.ClipData.newUri(requireContext().contentResolver,
                                getString(R.string.quotes_share), action.uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }, getString(R.string.quotes_share)))
                    } catch (_: ActivityNotFoundException) { toast(R.string.quotes_export_failed) }
                }
            }
        }
    }

    private fun savePoster() { binding.poster.snapshot()?.let(viewModel::save) }
    private fun toast(message: Int) { Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show() }
}

package com.cute.wallpaper.ringtones.presentation.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.cute.wallpaper.ringtones.R
import com.cute.wallpaper.ringtones.databinding.FragmentTabRingtonesBinding
import com.cute.wallpaper.ringtones.databinding.ItemCategoryTabBinding
import com.cute.wallpaper.ringtones.presentation.base.BaseFragment
import com.cute.wallpaper.ringtones.presentation.main.MainTab
import com.cute.wallpaper.ringtones.presentation.main.MainViewModel
import com.cute.wallpaper.ringtones.presentation.ringtone.RingtoneActionEvent
import com.cute.wallpaper.ringtones.presentation.ringtone.RingtoneViewModel
import com.cute.wallpaper.ringtones.presentation.search.SearchMode
import com.cute.wallpaper.ringtones.presentation.search.SearchViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TabRingtonesFragment : BaseFragment<FragmentTabRingtonesBinding>() {

    private val viewModel: HomeViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels(ownerProducer = { requireParentFragment() })
    private val ringtoneViewModel: RingtoneViewModel by viewModels()
    private val collectionPagerCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            RingtoneCategory.pages.getOrNull(position)?.let(viewModel::selectRingtoneCategory)
        }
    }

    private val writeSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        ringtoneViewModel.retryPendingSet()
    }

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            ringtoneViewModel.retryPendingSet()
        } else {
            showToast(R.string.ringtone_storage_permission_required)
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentTabRingtonesBinding {
        return FragmentTabRingtonesBinding.inflate(inflater, container, false)
    }

    override fun initView() {
        binding.collectionPager.apply {
            adapter = RingtoneCategoryPagerAdapter(
                childFragmentManager,
                viewLifecycleOwner.lifecycle
            )
            isSaveEnabled = false
            isUserInputEnabled = true
            offscreenPageLimit = RingtoneCategory.pages.lastIndex
            val initialPage = RingtoneCategory.pages.indexOf(
                viewModel.selectedRingtoneCategory.value ?: RingtoneCategory.RINGTONES
            )
            setCurrentItem(initialPage, false)
            registerOnPageChangeCallback(collectionPagerCallback)
        }

        binding.ringtoneTabs.removeAllViews()
        RingtoneCategory.pages.forEachIndexed { index, category ->
            val tabView = createRingtoneTabView(category).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dp(36)
                ).apply {
                    marginStart = if (index == 0) 0 else dp(12)
                }
                setOnClickListener {
                    viewModel.selectRingtoneCategory(category)
                }
            }
            binding.ringtoneTabs.addView(tabView)
        }
        renderRingtoneTabs(binding.collectionPager.currentItem)
    }

    override fun initListener() {
        binding.etSearch.doAfterTextChanged {
            viewModel.updateSearchQuery(it?.toString().orEmpty())
        }
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId != EditorInfo.IME_ACTION_SEARCH) return@setOnEditorActionListener false
            openSearch()
            true
        }
    }

    override fun observeData() {
        viewModel.searchQuery.observe(viewLifecycleOwner) { query ->
            if (binding.etSearch.text?.toString() != query) {
                binding.etSearch.setText(query)
                binding.etSearch.setSelection(query.length)
            }
        }
        viewModel.selectedRingtoneCategory.observe(viewLifecycleOwner) { category ->
            val page = RingtoneCategory.pages.indexOf(category)
            if (page >= 0 && binding.collectionPager.currentItem != page) {
                binding.collectionPager.setCurrentItem(page, true)
            }
            if (page >= 0) renderRingtoneTabs(page)
        }
        mainViewModel.bottomContentPadding.observe(viewLifecycleOwner) {
            viewModel.updateBottomContentPadding(it)
        }
        ringtoneViewModel.actionEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let(::handleRingtoneAction)
        }
    }

    private fun createRingtoneTabView(category: RingtoneCategory): ViewGroup {
        val tabBinding = ItemCategoryTabBinding.inflate(
            layoutInflater,
            binding.ringtoneTabs,
            false
        )
        tabBinding.tabIcon.setImageResource(category.iconRes)
        tabBinding.tabTitle.setText(category.titleRes)
        tabBinding.root.contentDescription = getString(category.titleRes)
        return tabBinding.root
    }

    private fun renderRingtoneTabs(selectedPosition: Int) {
        RingtoneCategory.pages.forEachIndexed { index, category ->
            val tabView = binding.ringtoneTabs.getChildAt(index) ?: return@forEachIndexed
            val tabBinding = ItemCategoryTabBinding.bind(tabView)
            val selected = index == selectedPosition
            tabBinding.root.isSelected = selected
            tabBinding.root.setPaddingRelative(0, 0, dp(if (selected) 12 else 0), 0)
            tabBinding.tabIcon.setImageResource(category.iconRes)
            tabBinding.tabTitle.isVisible = selected
            tabBinding.tabTitle.setText(category.titleRes)
            tabBinding.root.elevation = if (selected) dp(2).toFloat() else 0f
        }
        binding.ringtoneTabs.requestLayout()
    }

    private fun openSearch() {
        val args = Bundle().apply {
            putString(SearchViewModel.ARG_MODE, SearchMode.KEYWORD.name)
            putString(SearchViewModel.ARG_MAIN_TAB, MainTab.RINGTONES.name)
            putString(SearchViewModel.ARG_QUERY, binding.etSearch.text?.toString().orEmpty())
        }
        binding.etSearch.clearFocus()
        (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
        navViewModel.navigate(R.id.searchFragment, args)
    }

    private fun handleRingtoneAction(event: RingtoneActionEvent) {
        when (event) {
            RingtoneActionEvent.RequestWriteSettings -> {
                showToast(R.string.ringtone_write_settings_required)
                writeSettingsLauncher.launch(
                    Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                        data = Uri.parse("package:${requireContext().packageName}")
                    }
                )
            }

            RingtoneActionEvent.RequestStoragePermission -> {
                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }

            is RingtoneActionEvent.SetSuccess -> showToast(R.string.ringtone_set_success)
            RingtoneActionEvent.SetFailed -> showToast(R.string.ringtone_set_failed)
            RingtoneActionEvent.PreviewFailed -> showToast(R.string.ringtone_preview_failed)
        }
    }

    override fun onPause() {
        ringtoneViewModel.stopPreview()
        super.onPause()
    }

    override fun onDestroyView() {
        binding.ringtoneTabs.removeAllViews()
        binding.collectionPager.unregisterOnPageChangeCallback(collectionPagerCallback)
        binding.collectionPager.adapter = null
        super.onDestroyView()
    }

    private fun showToast(messageRes: Int) {
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val ARG_MAIN_TAB = "main_tab"

        fun newInstance() = TabRingtonesFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_MAIN_TAB, MainTab.RINGTONES.name)
            }
        }
    }
}

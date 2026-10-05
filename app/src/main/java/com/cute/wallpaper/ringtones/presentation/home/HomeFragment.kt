package com.cute.wallpaper.ringtones.presentation.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import androidx.viewpager2.widget.ViewPager2
import com.cute.wallpaper.ringtones.R
import com.cute.wallpaper.ringtones.databinding.FragmentHomeBinding
import com.cute.wallpaper.ringtones.presentation.base.BaseFragment
import com.cute.wallpaper.ringtones.presentation.main.MainFragment
import com.cute.wallpaper.ringtones.presentation.main.MainTab
import com.cute.wallpaper.ringtones.presentation.main.MainViewModel
import com.cute.wallpaper.ringtones.presentation.main.hasCollections
import com.cute.wallpaper.ringtones.presentation.ringtone.RingtoneActionEvent
import com.cute.wallpaper.ringtones.presentation.ringtone.RingtoneViewModel
import com.cute.wallpaper.ringtones.presentation.search.SearchMode
import com.cute.wallpaper.ringtones.presentation.search.SearchViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.transition.MaterialSharedAxis
import com.google.android.material.transition.SlideDistanceProvider
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>() {
    private val viewModel: HomeViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels(ownerProducer = { requireParentFragment() })
    private val ringtoneViewModel: RingtoneViewModel by viewModels()
    private val tab: MainTab by lazy {
        MainTab.entries.firstOrNull { it.name == arguments?.getString(ARG_TAB) } ?: MainTab.WALLPAPERS
    }
    private val colorChips = mutableMapOf<String, Chip>()
    private val genreChips = mutableMapOf<String, Chip>()
    private var updatingFilterSelection = false
    private var ringtoneTabMediator: TabLayoutMediator? = null
    private var hasRenderedSearchFilterVisibility = false

    private val ringtoneTabSelectionListener = object : TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab) {
            renderRingtoneTabs(tab.position)
        }

        override fun onTabUnselected(tab: TabLayout.Tab) = Unit
        override fun onTabReselected(tab: TabLayout.Tab) = Unit
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

    private val collectionPagerCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            when {
                tab.hasCollections -> WallpaperCollection.pages.getOrNull(position)
                    ?.let(mainViewModel::selectCollection)

                tab == MainTab.RINGTONES -> RingtoneCategory.pages.getOrNull(position)
                    ?.let(viewModel::selectRingtoneCategory)
            }
        }
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(inflater, container, false)
    }

    override fun initView() {
        hasRenderedSearchFilterVisibility = false
        binding.etSearch.setHint(tab.searchHintRes)
        binding.ringtoneTabs.isVisible = tab == MainTab.RINGTONES
        binding.collectionPager.apply {
            adapter = HomeCollectionPagerAdapter(
                childFragmentManager,
                viewLifecycleOwner.lifecycle,
                tab
            )
            isSaveEnabled = false
            isUserInputEnabled = tab.hasCollections || tab == MainTab.RINGTONES
            if (tab.hasCollections) offscreenPageLimit = WallpaperCollection.pages.lastIndex
            if (tab == MainTab.RINGTONES) offscreenPageLimit = RingtoneCategory.pages.lastIndex
            val initialPage = when {
                tab.hasCollections -> WallpaperCollection.pages.indexOf(
                    mainViewModel.selectedCollection.value ?: WallpaperCollection.WALLPAPER
                )

                tab == MainTab.RINGTONES -> RingtoneCategory.pages.indexOf(
                    viewModel.selectedRingtoneCategory.value ?: RingtoneCategory.RINGTONES
                )

                else -> 0
            }
            setCurrentItem(initialPage, false)
            registerOnPageChangeCallback(collectionPagerCallback)
        }
        if (tab == MainTab.RINGTONES) {
            ringtoneTabMediator = TabLayoutMediator(
                binding.ringtoneTabs,
                binding.collectionPager
            ) { ringtoneTab, position ->
                ringtoneTab.customView = createRingtoneTabView(RingtoneCategory.pages[position])
            }.also { it.attach() }
            binding.ringtoneTabs.addOnTabSelectedListener(ringtoneTabSelectionListener)
            renderRingtoneTabs(binding.collectionPager.currentItem)
        }
    }

    override fun initListener() {
        binding.etSearch.doAfterTextChanged { viewModel.updateSearchQuery(it?.toString().orEmpty()) }
        binding.etSearch.setOnClickListener { viewModel.setSearchFiltersVisible(true) }
        binding.etSearch.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) viewModel.setSearchFiltersVisible(true)
        }
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId != EditorInfo.IME_ACTION_SEARCH) return@setOnEditorActionListener false
            openSearch(SearchMode.KEYWORD)
            true
        }
        binding.btnCloseSearchFilters.setOnClickListener { dismissSearchFilters() }
    }

    override fun observeData() {
        viewModel.searchQuery.observe(viewLifecycleOwner) { query ->
            if (binding.etSearch.text?.toString() != query) {
                binding.etSearch.setText(query)
                binding.etSearch.setSelection(query.length)
            }
        }
        viewModel.searchFilterOptions.observe(viewLifecycleOwner, ::renderSearchFilterOptions)
        viewModel.searchFiltersVisible.observe(viewLifecycleOwner, ::renderSearchFilterVisibility)
        viewModel.selectedColorId.observe(viewLifecycleOwner, ::updateColorSelection)
        viewModel.selectedGenreIds.observe(viewLifecycleOwner, ::updateGenreSelection)
        mainViewModel.selectedCollection.observe(viewLifecycleOwner) { collection ->
            val page = WallpaperCollection.pages.indexOf(collection)
            if (tab.hasCollections && binding.collectionPager.currentItem != page) {
                binding.collectionPager.setCurrentItem(page, true)
            }
        }
        viewModel.selectedRingtoneCategory.observe(viewLifecycleOwner) { category ->
            if (tab == MainTab.RINGTONES) {
                val page = RingtoneCategory.pages.indexOf(category)
                if (page >= 0 && binding.collectionPager.currentItem != page) {
                    binding.collectionPager.setCurrentItem(page, true)
                }
                if (page >= 0) renderRingtoneTabs(page)
            }
        }
        mainViewModel.bottomContentPadding.observe(viewLifecycleOwner) {
            viewModel.updateBottomContentPadding(it)
        }
        if (tab == MainTab.RINGTONES || tab == MainTab.FAVORITES) {
            ringtoneViewModel.actionEvent.observe(viewLifecycleOwner) { event ->
                event.getContentIfNotHandled()?.let(::handleRingtoneAction)
            }
        }
    }

    private fun createRingtoneTabView(category: RingtoneCategory): TextView {
        return TextView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(36))
            minWidth = dp(40)
            gravity = Gravity.CENTER
            setPadding(dp(12), 0, dp(12), 0)
            setTextAppearance(R.style.Body14M)
            setTextColor(ContextCompat.getColor(context, R.color.tertiary_600))
            setBackgroundResource(R.drawable.bg_ringtone_category_item)
            contentDescription = getString(category.titleRes)
        }
    }

    private fun renderRingtoneTabs(selectedPosition: Int) {
        if (tab != MainTab.RINGTONES) return
        RingtoneCategory.pages.forEachIndexed { index, category ->
            val tabView = binding.ringtoneTabs.getTabAt(index)?.customView as? TextView
                ?: return@forEachIndexed
            val selected = index == selectedPosition
            tabView.isSelected = selected
            tabView.text = if (selected) getString(category.titleRes) else ""
            val horizontalPadding = if (selected) dp(20) else 0
            tabView.setPadding(horizontalPadding, 0, horizontalPadding, 0)
            tabView.elevation = if (selected) dp(2).toFloat() else 0f
        }
        binding.ringtoneTabs.requestLayout()
    }

    private fun renderSearchFilterVisibility(isVisible: Boolean) {
        val panel = binding.searchFilterPanel
        if (panel.isVisible == isVisible) {
            hasRenderedSearchFilterVisibility = true
            return
        }
        if (hasRenderedSearchFilterVisibility && binding.root.isLaidOut) {
            val panelMotion = MaterialSharedAxis(MaterialSharedAxis.Y, !isVisible).apply {
                duration = SEARCH_FILTER_ANIMATION_DURATION
                addTarget(panel)
                (primaryAnimatorProvider as? SlideDistanceProvider)?.slideDistance = dp(24)
            }
            val contentMotion = ChangeBounds().apply {
                duration = SEARCH_FILTER_ANIMATION_DURATION
                addTarget(binding.collectionPager)
            }
            TransitionManager.beginDelayedTransition(
                binding.root,
                TransitionSet().apply {
                    ordering = TransitionSet.ORDERING_TOGETHER
                    addTransition(contentMotion)
                    addTransition(panelMotion)
                }
            )
        }
        panel.isVisible = isVisible
        hasRenderedSearchFilterVisibility = true
    }

    private fun renderSearchFilterOptions(model: SearchFilterUiModel) {
        colorChips.clear()
        binding.colorChipGroup.removeAllViews()
        model.colors.forEach { option ->
            val chip = layoutInflater.inflate(
                R.layout.item_search_color_chip,
                binding.colorChipGroup,
                false
            ) as Chip
            val fillColor = runCatching { Color.parseColor(option.colorHex) }
                .getOrDefault(ContextCompat.getColor(requireContext(), R.color.primary_100))
            val defaultStroke = if (option.id == "white") {
                ContextCompat.getColor(requireContext(), R.color.tertiary_200)
            } else {
                Color.TRANSPARENT
            }
            chip.id = View.generateViewId()
            chip.contentDescription = getString(R.string.home_filter_color_description, option.label)
            chip.chipBackgroundColor = ColorStateList.valueOf(fillColor)
            chip.chipStrokeColor = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(ContextCompat.getColor(requireContext(), R.color.primary_500), defaultStroke)
            )
            chip.isChecked = viewModel.selectedColorId.value == option.id
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (updatingFilterSelection) return@setOnCheckedChangeListener
                if (isChecked) {
                    viewModel.setSelectedColor(option.id)
                    openSearch(SearchMode.COLOR, option.id)
                } else if (viewModel.selectedColorId.value == option.id) {
                    viewModel.setSelectedColor(null)
                }
            }
            colorChips[option.id] = chip
            binding.colorChipGroup.addView(chip)
        }

        genreChips.clear()
        binding.genreChipGroup.removeAllViews()
        val selectedGenres = viewModel.selectedGenreIds.value.orEmpty().toSet()
        model.genres.forEach { option ->
            val chip = layoutInflater.inflate(
                R.layout.item_search_genre_chip,
                binding.genreChipGroup,
                false
            ) as Chip
            chip.id = View.generateViewId()
            chip.text = option.label
            chip.isChecked = option.id in selectedGenres
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (updatingFilterSelection) return@setOnCheckedChangeListener
                viewModel.setGenreSelected(option.id, isChecked)
                if (isChecked) openSearch(SearchMode.GENRE, option.id)
            }
            genreChips[option.id] = chip
            binding.genreChipGroup.addView(chip)
        }
    }

    private fun updateColorSelection(selectedColorId: String) {
        updatingFilterSelection = true
        try {
            colorChips.forEach { (colorId, chip) ->
                if (chip.isChecked != (colorId == selectedColorId)) {
                    chip.isChecked = colorId == selectedColorId
                }
            }
        } finally {
            updatingFilterSelection = false
        }
    }

    private fun updateGenreSelection(selectedGenreIds: List<String>) {
        val selected = selectedGenreIds.toSet()
        updatingFilterSelection = true
        try {
            genreChips.forEach { (genreId, chip) ->
                if (chip.isChecked != (genreId in selected)) {
                    chip.isChecked = genreId in selected
                }
            }
        } finally {
            updatingFilterSelection = false
        }
    }

    private fun dismissSearchFilters() {
        viewModel.setSearchFiltersVisible(false)
        binding.etSearch.clearFocus()
        val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
    }

    private fun openSearch(mode: SearchMode, selectedFilterId: String = "") {
        val args = Bundle().apply {
            putString(SearchViewModel.ARG_MODE, mode.name)
            putString(SearchViewModel.ARG_MAIN_TAB, tab.name)
            putString(SearchViewModel.ARG_QUERY, binding.etSearch.text?.toString().orEmpty())
            when (mode) {
                SearchMode.COLOR -> putString(SearchViewModel.ARG_COLOR_ID, selectedFilterId)
                SearchMode.GENRE -> putString(SearchViewModel.ARG_GENRE_ID, selectedFilterId)
                SearchMode.KEYWORD -> Unit
            }
        }
        dismissSearchFilters()
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

    private fun showToast(messageRes: Int) {
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
    }

    fun revealChromeIfActive(sourceTab: MainTab) {
        if (mainViewModel.selectedTab.value == sourceTab) {
            (parentFragment as? MainFragment)?.revealChrome()
        }
    }

    override fun onPause() {
        if (tab == MainTab.RINGTONES || tab == MainTab.FAVORITES) {
            ringtoneViewModel.stopPreview()
        }
        super.onPause()
    }

    override fun onDestroyView() {
        binding.ringtoneTabs.removeOnTabSelectedListener(ringtoneTabSelectionListener)
        ringtoneTabMediator?.detach()
        ringtoneTabMediator = null
        binding.collectionPager.unregisterOnPageChangeCallback(collectionPagerCallback)
        binding.collectionPager.adapter = null
        colorChips.clear()
        genreChips.clear()
        super.onDestroyView()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val ARG_TAB = "main_tab"
        private const val SEARCH_FILTER_ANIMATION_DURATION = 240L

        fun newInstance(tab: MainTab) = HomeFragment().apply {
            arguments = Bundle().apply { putString(ARG_TAB, tab.name) }
        }
    }
}

package com.cute.wallpaper.ringtones.presentation.home

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
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
import com.cute.wallpaper.ringtones.presentation.home.demo.DemoCollection
import com.cute.wallpaper.ringtones.presentation.main.MainFragment
import com.cute.wallpaper.ringtones.presentation.main.MainTab
import com.cute.wallpaper.ringtones.presentation.main.MainViewModel
import com.cute.wallpaper.ringtones.presentation.main.hasCollections
import com.cute.wallpaper.ringtones.presentation.search.SearchMode
import com.cute.wallpaper.ringtones.presentation.search.SearchViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.transition.MaterialSharedAxis
import com.google.android.material.transition.SlideDistanceProvider
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>() {
    private val viewModel: HomeViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels(ownerProducer = { requireParentFragment() })
    private val tab: MainTab by lazy {
        MainTab.entries.firstOrNull { it.name == arguments?.getString(ARG_TAB) } ?: MainTab.WALLPAPERS
    }
    private val colorChips = mutableMapOf<String, Chip>()
    private val genreChips = mutableMapOf<String, Chip>()
    private var updatingFilterSelection = false
    private var hasRenderedSearchFilterVisibility = false

    private val collectionPagerCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            if (!tab.hasCollections) return
            DemoCollection.pages.getOrNull(position)?.let(mainViewModel::selectCollection)
        }
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(inflater, container, false)
    }

    override fun initView() {
        hasRenderedSearchFilterVisibility = false
        binding.etSearch.setHint(tab.searchHintRes)
        binding.collectionPager.apply {
            adapter = HomeCollectionPagerAdapter(
                childFragmentManager,
                viewLifecycleOwner.lifecycle,
                tab
            )
            isSaveEnabled = false
            isUserInputEnabled = tab.hasCollections
            if (tab.hasCollections) offscreenPageLimit = DemoCollection.pages.lastIndex
            val initialPage = if (tab.hasCollections) {
                DemoCollection.pages.indexOf(
                    mainViewModel.selectedCollection.value?.canonical()
                        ?: DemoCollection.WALLPAPER
                )
            } else {
                0
            }
            setCurrentItem(initialPage, false)
            registerOnPageChangeCallback(collectionPagerCallback)
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
            val page = DemoCollection.pages.indexOf(collection.canonical())
            if (tab.hasCollections && binding.collectionPager.currentItem != page) {
                binding.collectionPager.setCurrentItem(page, true)
            }
        }
        mainViewModel.bottomContentPadding.observe(viewLifecycleOwner) {
            viewModel.updateBottomContentPadding(it)
        }
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

    fun revealChromeIfActive(sourceTab: MainTab) {
        if (mainViewModel.selectedTab.value == sourceTab) {
            (parentFragment as? MainFragment)?.revealChrome()
        }
    }

    override fun onDestroyView() {
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

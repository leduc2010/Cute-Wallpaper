package com.cute.wallpaper.ringtones.presentation.profile

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.view.doOnPreDraw
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.cute.wallpaper.ringtones.R
import com.cute.wallpaper.ringtones.databinding.FragmentTabProfilePicturesBinding
import com.cute.wallpaper.ringtones.presentation.base.BaseFragment
import com.cute.wallpaper.ringtones.presentation.home.HomeContentUiModel
import com.cute.wallpaper.ringtones.presentation.main.MainFragment
import com.cute.wallpaper.ringtones.presentation.main.MainTab
import com.cute.wallpaper.ringtones.presentation.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TabProfilePicturesFragment : BaseFragment<FragmentTabProfilePicturesBinding>() {
    private val viewModel: ProfilePicturesViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels(ownerProducer = { requireParentFragment() })
    private var feedAdapter: ProfilePicturesAdapter? = null
    private var renderedFiltersVisible = false
    private var pendingFilterReveal = false
    private var pendingChromeReveal = false
    private var renderGeneration = 0L

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentTabProfilePicturesBinding.inflate(inflater, container, false)

    override fun initView() {
        renderedFiltersVisible = false
        pendingFilterReveal = false
        pendingChromeReveal = false
        feedAdapter = ProfilePicturesAdapter(
            onOpen = ::openDetail,
            onFavorite = viewModel::setFavorite,
            onSurprise = { viewModel.visibleContents().randomOrNull()?.let(::openDetail) },
            onColor = viewModel::selectColor,
            onTag = viewModel::selectTag,
            onCloseFilters = ::dismissFilters
        ).apply { stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY }
        binding.rvContent.apply {
            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            adapter = feedAdapter
            itemAnimator = null
            addItemDecoration(ProfileSpacing(dp(12)))
        }
    }

    override fun initListener() {
        binding.etSearch.doAfterTextChanged { viewModel.updateQuery(it?.toString().orEmpty()) }
        binding.etSearch.setOnClickListener { viewModel.showFilters(true) }
        binding.etSearch.setOnFocusChangeListener { _, focused -> if (focused) viewModel.showFilters(true) }
        binding.etSearch.setOnEditorActionListener { _, action, event ->
            val hardwareEnter = event?.keyCode == KeyEvent.KEYCODE_ENTER
            if (hardwareEnter) {
                if (event?.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) dismissFilters()
                true
            } else if (action == EditorInfo.IME_ACTION_SEARCH) {
                dismissFilters()
                true
            } else false
        }
    }

    override fun observeData() {
        viewModel.searchState.observe(viewLifecycleOwner) { state ->
            if (binding.etSearch.text.toString() != state.query) {
                binding.etSearch.setText(state.query)
                binding.etSearch.setSelection(state.query.length)
            }
            renderFeed()
        }
        viewModel.favoriteKeys.observe(viewLifecycleOwner) { renderFeed() }
        mainViewModel.bottomContentPadding.observe(viewLifecycleOwner) { padding ->
            binding.rvContent.updatePadding(bottom = padding + dp(16))
        }
    }

    private fun renderFeed() {
        val generation = ++renderGeneration
        val state = viewModel.searchState.value ?: ProfileSearchState()
        if (state.filtersVisible && !renderedFiltersVisible) pendingFilterReveal = true
        if (!state.filtersVisible) pendingFilterReveal = false
        renderedFiltersVisible = state.filtersVisible
        val contents = viewModel.visibleContents()
        val rows = buildList {
            if (state.filtersVisible) {
                add(ProfileFeedRow.Filters(viewModel.availableTags, state.color, state.tags))
            }
            if (!state.isSearching && contents.isNotEmpty()) {
                val featured = contents.first()
                add(ProfileFeedRow.Featured(featured, featured.ref.toFavoriteKey() in viewModel.favoriteKeys.value.orEmpty()))
                add(ProfileFeedRow.Banner)
            }
            if (contents.isEmpty()) {
                add(ProfileFeedRow.Empty(state.isSearching))
                pendingChromeReveal = true
            } else {
                contents.forEachIndexed { index, content ->
                    // Both card presentations refer to individual RC items. No collection contract exists.
                    if (!state.isSearching && index % 4 == 1) {
                        add(ProfileFeedRow.AvatarCaption(content))
                    } else {
                        add(ProfileFeedRow.Avatar(content))
                    }
                }
            }
        }
        val adapter = feedAdapter ?: return
        adapter.submitList(rows) {
            if (generation != renderGeneration) return@submitList
            if (view != null && feedAdapter === adapter && pendingFilterReveal &&
                viewModel.searchState.value?.filtersVisible == true) {
                pendingFilterReveal = false
                binding.rvContent.scrollToPosition(0)
                pendingChromeReveal = true
            }
            if (view != null && feedAdapter === adapter && pendingChromeReveal) {
                // Wait for the filter-row removal/scroll layout to finish; revealing before it
                // lets that layout's nested scroll hide the shared shell again.
                binding.rvContent.doOnPreDraw {
                    if (generation == renderGeneration && view != null && feedAdapter === adapter && pendingChromeReveal &&
                        mainViewModel.selectedTab.value == MainTab.PROFILE_PICTURES) {
                        pendingChromeReveal = false
                        (parentFragment as? MainFragment)?.revealChrome()
                    }
                }
            }
        }
    }

    private fun dismissFilters() {
        val wasVisible = viewModel.searchState.value?.filtersVisible == true
        pendingChromeReveal = true
        viewModel.showFilters(false)
        if (!wasVisible) renderFeed()
        binding.etSearch.clearFocus()
        (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
    }

    private fun openDetail(content: HomeContentUiModel) {
        if (content.contentUrl.isNullOrBlank() || !isResumed) return
        dismissFilters()
        navViewModel.navigate(
            R.id.profilePictureDetailFragment,
            Bundle().apply {
                putString(ProfilePictureDetailViewModel.ARG_CONTENT_ID, content.id)
                putStringArrayList(
                    ProfilePictureDetailViewModel.ARG_CONTENT_IDS,
                    ArrayList(viewModel.visibleContents().map { it.id })
                )
            }
        )
    }

    override fun onDestroyView() {
        binding.rvContent.adapter = null
        feedAdapter = null
        pendingFilterReveal = false
        pendingChromeReveal = false
        super.onDestroyView()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private class ProfileSpacing(private val gap: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            val params = view.layoutParams as? StaggeredGridLayoutManager.LayoutParams ?: return
            if (params.isFullSpan) {
                outRect.left = gap / 3
                outRect.right = gap / 3
            } else {
                outRect.left = if (params.spanIndex == 1) gap / 2 else 0
                outRect.right = if (params.spanIndex == 0) gap / 2 else 0
            }
            outRect.bottom = gap
        }
    }
}

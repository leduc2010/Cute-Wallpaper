package com.cute.wallpaper.ringtones.presentation.quotes

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cute.wallpaper.ringtones.R
import com.cute.wallpaper.ringtones.databinding.FragmentTabQuotesBinding
import com.cute.wallpaper.ringtones.databinding.ItemCategoryTabBinding
import com.cute.wallpaper.ringtones.presentation.base.BaseFragment
import com.cute.wallpaper.ringtones.presentation.home.HomeContentUiModel
import com.cute.wallpaper.ringtones.presentation.main.MainFragment
import com.cute.wallpaper.ringtones.presentation.main.MainTab
import com.cute.wallpaper.ringtones.presentation.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TabQuotesFragment : BaseFragment<FragmentTabQuotesBinding>() {
    private val viewModel: QuotesViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels(ownerProducer = { requireParentFragment() })
    private var feedAdapter: QuotesFeedAdapter? = null
    private var changingSearch = false
    private var categoryBack: OnBackPressedCallback? = null
    private var keyboardVisible = false

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentTabQuotesBinding.inflate(inflater, container, false)

    override fun initView() {
        val adapter = QuotesFeedAdapter(viewModel::setFavorite, ::openDetail,
            { QuoteActions.copy(requireContext(), it) }, { QuoteActions.shareText(requireContext(), it) },
            { dismissKeyboard(); viewModel.selectCategory(it); binding.rvContent.scrollToPosition(0) })
        feedAdapter = adapter
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val visible = insets.isVisible(WindowInsetsCompat.Type.ime())
            if (keyboardVisible && !visible) revealEmptyFeedNavigation()
            keyboardVisible = visible
            insets
        }
        adapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        binding.rvContent.apply {
            layoutManager = GridLayoutManager(requireContext(), 2).apply {
                spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int) = if (adapter.isFullSpan(position)) 2 else 1
                }
            }
            this.adapter = adapter
            itemAnimator = null
            addItemDecoration(QuoteSpacing(dp(12)))
        }
        categoryBack = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() { viewModel.showCategories() }
        }.also { requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, it) }

        binding.quoteTabs.removeAllViews()
        QuotesTab.entries.forEachIndexed { index, tab ->
            val item = ItemCategoryTabBinding.inflate(
                layoutInflater,
                binding.quoteTabs,
                false
            )
            item.tabIcon.setImageResource(tab.iconRes)
            item.tabTitle.setText(tab.titleRes)
            item.root.apply {
                contentDescription = getString(tab.titleRes)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dp(40)
                ).apply {
                    marginStart = if (index == 0) 0 else dp(10)
                }
                setOnClickListener { selectTab(tab) }
            }
            binding.quoteTabs.addView(item.root)
        }
    }

    override fun initListener() {
        binding.btnCategoryBrowser.setOnClickListener { dismissKeyboard(); viewModel.showCategories() }
        binding.etSearch.doAfterTextChanged { if (!changingSearch) viewModel.updateQuery(it?.toString().orEmpty()) }
        binding.etSearch.setOnEditorActionListener { _, action, event ->
            when {
                event?.keyCode == KeyEvent.KEYCODE_ENTER -> {
                    if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) dismissKeyboard()
                    true
                }
                action == EditorInfo.IME_ACTION_SEARCH -> { dismissKeyboard(); true }
                else -> false
            }
        }
    }

    override fun observeData() {
        viewModel.state.observe(viewLifecycleOwner) { render() }
        viewModel.favoriteKeys.observe(viewLifecycleOwner) { render() }
        mainViewModel.selectedTab.observe(viewLifecycleOwner) { renderBackEnabled() }
        mainViewModel.bottomContentPadding.observe(viewLifecycleOwner) {
            binding.rvContent.updatePadding(bottom = it + dp(16))
        }
    }

    private fun selectTab(tab: QuotesTab) {
        dismissKeyboard()
        viewModel.selectTab(tab)
        binding.rvContent.scrollToPosition(0)
    }

    private fun render() {
        val state = viewModel.state.value ?: QuotesState()
        renderTabs(state.tab)
        if (binding.etSearch.text.toString() != state.query) {
            changingSearch = true
            binding.etSearch.setText(state.query)
            binding.etSearch.setSelection(state.query.length)
            changingSearch = false
        }
        binding.btnCategoryBrowser.isVisible = state.categoryId != null
        binding.etSearch.updatePadding(right = dp(if (state.categoryId == null) 12 else 120))
        renderBackEnabled()
        val favorites = viewModel.favoriteKeys.value.orEmpty()
        val rows = buildList {
            if (state.tab == QuotesTab.CATEGORIES && state.categoryId == null) {
                val categories = viewModel.visibleCategories()
                if (categories.isEmpty()) add(QuoteFeedRow.Empty(state.query.isNotBlank())) else {
                    if (state.query.isBlank()) add(QuoteFeedRow.FeaturedCategory(categories.first()))
                    categories.forEach { add(QuoteFeedRow.Category(it)) }
                }
            } else {
                val quotes = viewModel.visibleQuotes()
                if (quotes.isEmpty()) add(QuoteFeedRow.Empty(state.query.isNotBlank())) else {
                    if (state.tab != QuotesTab.CATEGORIES && state.query.isBlank()) {
                        val featured = quotes.first()
                        add(QuoteFeedRow.Featured(QuoteCard(featured, featured.ref.toFavoriteKey() in favorites)))
                    }
                    quotes.forEach { add(QuoteFeedRow.Card(QuoteCard(it, it.ref.toFavoriteKey() in favorites))) }
                }
            }
        }
        feedAdapter?.submitList(rows) { revealEmptyFeedNavigation() }
    }

    private fun revealEmptyFeedNavigation() {
        if (feedAdapter?.currentList?.firstOrNull() is QuoteFeedRow.Empty &&
            mainViewModel.selectedTab.value == MainTab.QUOTES && view != null) {
            (parentFragment as? MainFragment)?.revealChrome(animate = false)
        }
    }

    private fun renderTabs(selectedTab: QuotesTab) {
        QuotesTab.entries.forEachIndexed { index, tab ->
            val tabView = binding.quoteTabs.getChildAt(index) ?: return@forEachIndexed
            val item = ItemCategoryTabBinding.bind(tabView)
            val selected = tab == selectedTab

            item.root.isSelected = selected
            item.root.setPaddingRelative(0, 0, dp(if (selected) 12 else 0), 0)
            item.tabTitle.isVisible = selected
        }
        binding.quoteTabs.requestLayout()
    }

    private fun renderBackEnabled() {
        categoryBack?.isEnabled = mainViewModel.selectedTab.value == MainTab.QUOTES &&
            viewModel.state.value?.categoryId != null
    }

    private fun openDetail(item: HomeContentUiModel) {
        if (!isResumed || item.quote.isNullOrBlank()) return
        dismissKeyboard()
        navViewModel.navigate(R.id.quoteDetailFragment, Bundle().apply {
            putString(QuoteDetailViewModel.ARG_CONTENT_ID, item.id)
        })
    }

    private fun dismissKeyboard() {
        binding.etSearch.clearFocus()
        (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
    }

    override fun onDestroyView() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root, null)
        keyboardVisible = false
        categoryBack = null
        binding.quoteTabs.removeAllViews()
        binding.rvContent.adapter = null
        feedAdapter = null
        super.onDestroyView()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private class QuoteSpacing(private val gap: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            val params = view.layoutParams as? GridLayoutManager.LayoutParams ?: return
            if (params.spanSize == 1) {
                outRect.left = if (params.spanIndex == 1) gap / 2 else 0
                outRect.right = if (params.spanIndex == 0) gap / 2 else 0
            }
            outRect.bottom = gap
        }
    }
}

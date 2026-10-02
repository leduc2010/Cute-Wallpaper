package com.cute.wallpaper.ringtones.presentation.search

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cute.wallpaper.ringtones.R
import com.cute.wallpaper.ringtones.core.model.ContentType
import com.cute.wallpaper.ringtones.data.fake.FakeContentDataSource
import com.cute.wallpaper.ringtones.databinding.FragmentSearchResultBinding
import com.cute.wallpaper.ringtones.presentation.base.BaseFragment
import com.cute.wallpaper.ringtones.presentation.detail.WallpaperDetailViewModel
import com.cute.wallpaper.ringtones.presentation.home.HomeContentUiModel
import com.cute.wallpaper.ringtones.presentation.home.demo.DemoArtwork
import com.cute.wallpaper.ringtones.presentation.home.demo.DemoContentAdapter
import com.cute.wallpaper.ringtones.presentation.home.demo.DemoContentCard
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SearchResultFragment : BaseFragment<FragmentSearchResultBinding>() {
    @Inject lateinit var fakeContentDataSource: FakeContentDataSource

    private val viewModel: SearchViewModel by viewModels(ownerProducer = { requireParentFragment() })
    private val page: SearchPageUiModel by lazy {
        SearchPageUiModel(
            id = requireArguments().getString(ARG_PAGE_ID).orEmpty(),
            label = requireArguments().getString(ARG_PAGE_LABEL).orEmpty(),
            emoji = requireArguments().getString(ARG_PAGE_EMOJI).orEmpty(),
            kind = SearchPageKind.entries.firstOrNull {
                it.name == requireArguments().getString(ARG_PAGE_KIND)
            } ?: SearchPageKind.GENRE,
            colorHex = requireArguments().getString(ARG_PAGE_COLOR).orEmpty()
        )
    }
    private var contentAdapter: DemoContentAdapter? = null
    private var infoDialog: AlertDialog? = null

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSearchResultBinding {
        return FragmentSearchResultBinding.inflate(inflater, container, false)
    }

    override fun initListener() {
        binding.clearSearchButton.setOnClickListener { viewModel.updateQuery("") }
    }

    override fun initView() {
        val artwork = DemoArtwork(resources, fakeContentDataSource.artworkRegions)
        contentAdapter = DemoContentAdapter(artwork, { content, favorite ->
            viewModel.setFavorite(content.ref, favorite)
        }, ::openDetail, compactArtwork = true).apply {
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
        binding.rvContent.apply {
            layoutManager = GridLayoutManager(requireContext(), SEARCH_COLUMN_COUNT)
            adapter = contentAdapter
            itemAnimator = null
            updatePadding(bottom = dp(24))
            addItemDecoration(GridSpacing(dp(10)))
        }
    }

    override fun observeData() {
        viewModel.contentItems.observe(viewLifecycleOwner) { renderContent() }
        viewModel.query.observe(viewLifecycleOwner) { renderContent() }
        viewModel.favoriteKeys.observe(viewLifecycleOwner) { renderContent() }
    }

    private fun renderContent() {
        val favorites = viewModel.favoriteKeys.value.orEmpty()
        val cards = viewModel.filteredItems(page).map { content ->
            DemoContentCard(content, content.ref.toFavoriteKey() in favorites)
        }
        contentAdapter?.submitList(cards)
        val empty = cards.isEmpty()
        binding.rvContent.isVisible = !empty
        binding.searchEmptyState.isVisible = empty
    }

    private fun showComingSoon() {
        infoDialog?.dismiss()
        infoDialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.home_demo_title)
            .setMessage(R.string.home_demo_message)
            .setPositiveButton(R.string.home_preview_close, null)
            .show()
    }

    private fun openDetail(content: HomeContentUiModel) {
        if (content.type != ContentType.WALLPAPER || content.artworkIndex == null) {
            return showComingSoon()
        }
        navViewModel.navigate(
            R.id.wallpaperDetailFragment,
            Bundle().apply {
                putString(WallpaperDetailViewModel.ARG_CONTENT_ID, content.id)
                putString(WallpaperDetailViewModel.ARG_CONTENT_TYPE, content.type.name)
            }
        )
    }

    override fun onDestroyView() {
        infoDialog?.dismiss()
        infoDialog = null
        binding.rvContent.adapter = null
        contentAdapter = null
        super.onDestroyView()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private class GridSpacing(private val gap: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position = parent.getChildAdapterPosition(view)
            if (position == RecyclerView.NO_POSITION) return
            val column = position % SEARCH_COLUMN_COUNT
            outRect.left = gap * column / SEARCH_COLUMN_COUNT
            outRect.right = gap * (SEARCH_COLUMN_COUNT - 1 - column) / SEARCH_COLUMN_COUNT
            outRect.bottom = gap
        }
    }

    companion object {
        private const val ARG_PAGE_ID = "search_page_id"
        private const val ARG_PAGE_LABEL = "search_page_label"
        private const val ARG_PAGE_EMOJI = "search_page_emoji"
        private const val ARG_PAGE_KIND = "search_page_kind"
        private const val ARG_PAGE_COLOR = "search_page_color"
        private const val SEARCH_COLUMN_COUNT = 3

        fun newInstance(page: SearchPageUiModel) = SearchResultFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_PAGE_ID, page.id)
                putString(ARG_PAGE_LABEL, page.label)
                putString(ARG_PAGE_EMOJI, page.emoji)
                putString(ARG_PAGE_KIND, page.kind.name)
                putString(ARG_PAGE_COLOR, page.colorHex)
            }
        }
    }
}

package com.cute.wallpaper.ringtones.presentation.video

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cute.wallpaper.ringtones.R
import com.cute.wallpaper.ringtones.databinding.FragmentTabVideoWallpapersBinding
import com.cute.wallpaper.ringtones.presentation.base.BaseFragment
import com.cute.wallpaper.ringtones.presentation.detail.VideoWallpaperDetailViewModel
import com.cute.wallpaper.ringtones.presentation.home.ContentCard
import com.cute.wallpaper.ringtones.presentation.main.MainFragment
import com.cute.wallpaper.ringtones.presentation.main.MainTab
import com.cute.wallpaper.ringtones.presentation.main.MainViewModel
import com.cute.wallpaper.ringtones.presentation.search.SearchMode
import com.cute.wallpaper.ringtones.presentation.search.SearchViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TabVideoWallpapersFragment : BaseFragment<FragmentTabVideoWallpapersBinding>() {

    private val viewModel: VideoWallpapersViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels(ownerProducer = { requireParentFragment() })
    private var contentAdapter: VideoWallpapersAdapter? = null

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentTabVideoWallpapersBinding {
        return FragmentTabVideoWallpapersBinding.inflate(inflater, container, false)
    }

    override fun initView() {
        binding.etSearch.apply {
            isFocusable = false
            isCursorVisible = false
            setHint(R.string.search_video_wallpapers)
        }

        contentAdapter = VideoWallpapersAdapter(
            onFavorite = { content, favorite ->
                viewModel.setFavorite(content.ref, favorite)
            },
            onOpen = ::openDetail
        )

        binding.rvContent.apply {
            val grid = GridLayoutManager(requireContext(), VideoWallpapersAdapter.GRID_SPAN_COUNT)
            grid.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int =
                    contentAdapter?.spanSize(position) ?: VideoWallpapersAdapter.GRID_SPAN_COUNT
            }
            layoutManager = grid
            adapter = contentAdapter
            itemAnimator = null
            addItemDecoration(VideoGridSpacing(dp(12)))
        }

        renderContent()
    }

    override fun initListener() {
        binding.etSearch.setOnClickListener { openSearch() }
    }

    override fun observeData() {
        viewModel.favoriteKeys.observe(viewLifecycleOwner) {
            renderContent()
        }
        viewModel.featuredMediaInfo.observe(viewLifecycleOwner) {
            renderContent()
        }
        mainViewModel.bottomContentPadding.observe(viewLifecycleOwner) { padding ->
            binding.rvContent.updatePadding(bottom = padding + dp(16))
        }
    }

    private fun renderContent() {
        val favorites = viewModel.favoriteKeys.value.orEmpty()
        val featuredInfo = viewModel.featuredMediaInfo.value.orEmpty()
        val cards = viewModel.items.map { item ->
            val mediaInfo = featuredInfo[item.id]
            ContentCard(
                content = item,
                isFavorite = item.ref.toFavoriteKey() in favorites,
                durationMs = mediaInfo?.durationMs ?: 0L,
                qualityLabel = mediaInfo?.qualityLabel
            )
        }
        contentAdapter?.submitCards(cards)
        binding.rvContent.isVisible = cards.isNotEmpty()
        binding.emptyState.isVisible = cards.isEmpty()
        if (cards.isEmpty()) {
            (parentFragment as? MainFragment)?.revealChrome()
        }
    }

    private fun openSearch() {
        navViewModel.navigate(
            R.id.searchFragment,
            Bundle().apply {
                putString(SearchViewModel.ARG_MODE, SearchMode.KEYWORD.name)
                putString(SearchViewModel.ARG_MAIN_TAB, MainTab.VIDEO_WALLPAPERS.name)
                putString(SearchViewModel.ARG_QUERY, "")
            }
        )
    }

    private fun openDetail(content: com.cute.wallpaper.ringtones.presentation.home.HomeContentUiModel) {
        navViewModel.navigate(
            R.id.videoWallpaperDetailFragment,
            Bundle().apply {
                putString(VideoWallpaperDetailViewModel.ARG_CONTENT_ID, content.id)
            }
        )
    }

    override fun onDestroyView() {
        binding.rvContent.adapter = null
        contentAdapter = null
        super.onDestroyView()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private inner class VideoGridSpacing(
        private val gap: Int
    ) : RecyclerView.ItemDecoration() {

        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position = parent.getChildAdapterPosition(view)
            if (position == RecyclerView.NO_POSITION || contentAdapter?.isForYou(position) != true) {
                return
            }
            val layoutManager = parent.layoutManager as? GridLayoutManager ?: return
            val column = layoutManager.spanSizeLookup.getSpanIndex(
                position,
                VideoWallpapersAdapter.GRID_SPAN_COUNT
            )
            when (column) {
                0 -> outRect.right = gap * 2 / 3
                1 -> {
                    outRect.left = gap / 3
                    outRect.right = gap / 3
                }
                else -> outRect.left = gap * 2 / 3
            }
            outRect.bottom = gap
        }
    }
}

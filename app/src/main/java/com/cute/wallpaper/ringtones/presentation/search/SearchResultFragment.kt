package com.cute.wallpaper.ringtones.presentation.search

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
import com.cute.wallpaper.ringtones.domain.model.ContentType
import com.cute.wallpaper.ringtones.domain.model.RingtoneTarget
import com.cute.wallpaper.ringtones.databinding.FragmentSearchResultBinding
import com.cute.wallpaper.ringtones.presentation.base.BaseFragment
import com.cute.wallpaper.ringtones.presentation.detail.VideoWallpaperDetailViewModel
import com.cute.wallpaper.ringtones.presentation.detail.WallpaperDetailViewModel
import com.cute.wallpaper.ringtones.presentation.home.HomeContentUiModel
import com.cute.wallpaper.ringtones.presentation.home.ContentAdapter
import com.cute.wallpaper.ringtones.presentation.home.ContentCard
import com.cute.wallpaper.ringtones.presentation.main.MainTab
import com.cute.wallpaper.ringtones.presentation.profile.ProfilePictureDetailViewModel
import com.cute.wallpaper.ringtones.presentation.ringtone.RingtoneTargetBottomSheet
import com.cute.wallpaper.ringtones.presentation.ringtone.RingtoneViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchResultFragment : BaseFragment<FragmentSearchResultBinding>() {
    private val viewModel: SearchViewModel by viewModels(ownerProducer = { requireParentFragment() })
    private val ringtoneViewModel: RingtoneViewModel by viewModels(ownerProducer = { requireParentFragment() })
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
    private var contentAdapter: ContentAdapter? = null

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
        childFragmentManager.setFragmentResultListener(
            RingtoneTargetBottomSheet.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, result ->
            val contentId = result.getString(RingtoneTargetBottomSheet.RESULT_CONTENT_ID).orEmpty()
            val target = RingtoneTarget.entries.firstOrNull {
                it.name == result.getString(RingtoneTargetBottomSheet.RESULT_TARGET)
            } ?: return@setFragmentResultListener
            ringtoneViewModel.requestSet(contentId, target)
        }

        contentAdapter = ContentAdapter(
            onFavorite = { content, favorite ->
                viewModel.setFavorite(content.ref, favorite)
            },
            onPreview = ::openDetail,
            compactArtwork = true,
            onRingtoneSet = ::showRingtoneTarget
        ).apply {
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
        binding.rvContent.apply {
            val columns = if (viewModel.tab == MainTab.RINGTONES) 1 else SEARCH_COLUMN_COUNT
            layoutManager = GridLayoutManager(requireContext(), columns)
            adapter = contentAdapter
            itemAnimator = null
            updatePadding(bottom = dp(24))
            addItemDecoration(GridSpacing(dp(12), if (viewModel.tab == MainTab.RINGTONES) 1 else SEARCH_COLUMN_COUNT))
        }
    }

    override fun observeData() {
        viewModel.contentItems.observe(viewLifecycleOwner) { renderContent() }
        viewModel.query.observe(viewLifecycleOwner) { renderContent() }
        viewModel.favoriteKeys.observe(viewLifecycleOwner) { renderContent() }
        ringtoneViewModel.playbackState.observe(viewLifecycleOwner) { renderContent() }
    }

    private fun renderContent() {
        val favorites = viewModel.favoriteKeys.value.orEmpty()
        val cards = viewModel.filteredItems(page).map { content ->
            val playback = ringtoneViewModel.playbackState.value
            val active = playback?.contentId == content.id
            ContentCard(
                content = content,
                isFavorite = content.ref.toFavoriteKey() in favorites,
                isPlaying = active && playback?.isPlaying == true,
                isPreparing = active && playback?.isPreparing == true,
                playbackProgress = if (active) playback?.progress ?: 0 else 0
            )
        }
        contentAdapter?.submitList(cards)
        val empty = cards.isEmpty()
        binding.rvContent.isVisible = !empty
        binding.searchEmptyState.isVisible = empty
    }

    private fun openDetail(content: HomeContentUiModel) {
        if (content.contentUrl.isNullOrBlank()) return
        when (content.type) {
            ContentType.WALLPAPER -> navViewModel.navigate(
                R.id.wallpaperDetailFragment,
                Bundle().apply {
                    putString(WallpaperDetailViewModel.ARG_CONTENT_ID, content.id)
                    putString(WallpaperDetailViewModel.ARG_CONTENT_TYPE, content.type.name)
                }
            )

            ContentType.VIDEO_WALLPAPER -> navViewModel.navigate(
                R.id.videoWallpaperDetailFragment,
                Bundle().apply {
                    putString(VideoWallpaperDetailViewModel.ARG_CONTENT_ID, content.id)
                }
            )

            ContentType.PROFILE_PICTURE -> navViewModel.navigate(
                R.id.profilePictureDetailFragment,
                Bundle().apply {
                    putString(ProfilePictureDetailViewModel.ARG_CONTENT_ID, content.id)
                }
            )
            ContentType.RINGTONE -> ringtoneViewModel.togglePreview(content.id)
            else -> Unit
        }
    }

    private fun showRingtoneTarget(content: HomeContentUiModel) {
        if (content.type != ContentType.RINGTONE) return
        if (childFragmentManager.findFragmentByTag(RingtoneTargetBottomSheet.TAG) != null) return
        RingtoneTargetBottomSheet.newInstance(content.id).show(
            childFragmentManager,
            RingtoneTargetBottomSheet.TAG
        )
    }

    override fun onDestroyView() {
        binding.rvContent.adapter = null
        contentAdapter = null
        super.onDestroyView()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private class GridSpacing(
        private val gap: Int,
        private val columns: Int
    ) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position = parent.getChildAdapterPosition(view)
            if (position == RecyclerView.NO_POSITION) return
            val column = position % columns
            outRect.left = if (columns <= 1) 0 else gap * column / columns
            outRect.right = if (columns <= 1) 0 else gap * (columns - 1 - column) / columns
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

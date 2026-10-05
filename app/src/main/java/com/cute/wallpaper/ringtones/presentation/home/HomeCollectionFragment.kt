package com.cute.wallpaper.ringtones.presentation.home

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cute.wallpaper.ringtones.R
import com.cute.wallpaper.ringtones.domain.model.ContentType
import com.cute.wallpaper.ringtones.domain.model.RingtoneTarget
import com.cute.wallpaper.ringtones.databinding.FragmentHomeCollectionBinding
import com.cute.wallpaper.ringtones.presentation.base.BaseFragment
import com.cute.wallpaper.ringtones.presentation.detail.VideoWallpaperDetailViewModel
import com.cute.wallpaper.ringtones.presentation.detail.WallpaperDetailViewModel
import com.cute.wallpaper.ringtones.presentation.main.MainTab
import com.cute.wallpaper.ringtones.presentation.main.hasCollections
import com.cute.wallpaper.ringtones.presentation.profile.ProfilePictureDetailViewModel
import com.cute.wallpaper.ringtones.presentation.ringtone.RingtoneTargetBottomSheet
import com.cute.wallpaper.ringtones.presentation.ringtone.RingtoneViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeCollectionFragment : BaseFragment<FragmentHomeCollectionBinding>() {
    private val viewModel: HomeViewModel by viewModels(ownerProducer = { requireParentFragment() })
    private val ringtoneViewModel: RingtoneViewModel by viewModels(ownerProducer = { requireParentFragment() })
    private val tab: MainTab by lazy {
        MainTab.entries.firstOrNull { it.name == arguments?.getString(ARG_TAB) } ?: MainTab.WALLPAPERS
    }
    private val collection: WallpaperCollection by lazy {
        WallpaperCollection.entries.firstOrNull {
            it.name == arguments?.getString(ARG_COLLECTION)
        } ?: WallpaperCollection.WALLPAPER
    }
    private val ringtoneCategory: RingtoneCategory by lazy {
        RingtoneCategory.entries.firstOrNull {
            it.name == arguments?.getString(ARG_RINGTONE_CATEGORY)
        } ?: RingtoneCategory.RINGTONES
    }
    private var contentAdapter: ContentAdapter? = null
    private var ringtoneFeedAdapter: RingtoneFeedAdapter? = null

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentHomeCollectionBinding {
        return FragmentHomeCollectionBinding.inflate(inflater, container, false)
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

        if (tab == MainTab.RINGTONES) {
            ringtoneFeedAdapter = RingtoneFeedAdapter(
                onFavorite = { content, favorite ->
                    viewModel.setFavorite(content.ref, favorite)
                },
                onPreview = { ringtoneViewModel.togglePreview(it.id) },
                onSet = ::showRingtoneTarget
            ).apply {
                stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }
            binding.rvContent.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = ringtoneFeedAdapter
                itemAnimator = null
            }
        } else {
            contentAdapter = ContentAdapter(
                onFavorite = { content, favorite ->
                    viewModel.setFavorite(content.ref, favorite)
                },
                onPreview = ::openDetail,
                onRingtoneSet = ::showRingtoneTarget
            ).apply {
                stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }
            binding.rvContent.apply {
                layoutManager = GridLayoutManager(requireContext(), 2)
                adapter = contentAdapter
                itemAnimator = null
                addItemDecoration(GridSpacing(dp(16), 2))
            }
        }
    }

    override fun observeData() {
        viewModel.contentItems.observe(viewLifecycleOwner) { renderContent() }
        viewModel.searchQuery.observe(viewLifecycleOwner) { renderContent() }
        viewModel.favoriteKeys.observe(viewLifecycleOwner) { renderContent() }
        ringtoneViewModel.playbackState.observe(viewLifecycleOwner) { renderContent() }
        viewModel.bottomContentPadding.observe(viewLifecycleOwner) { padding ->
            binding.rvContent.updatePadding(bottom = padding + dp(16))
        }
    }

    private fun renderContent() {
        val favorites = viewModel.favoriteKeys.value.orEmpty()
        val query = viewModel.searchQuery.value.orEmpty().trim()
        val cards = viewModel.contentItems.value.orEmpty().filter { item ->
            val matchesTab = if (tab == MainTab.FAVORITES) {
                item.ref.toFavoriteKey() in favorites
            } else {
                item.type == tab.contentType
            }
            val matchesCollection = !tab.hasCollections || item.collection == collection
            val matchesRingtoneCategory = tab != MainTab.RINGTONES ||
                item.category == ringtoneCategory.remoteKey
            val matchesSearch = item.matches(query)
            matchesTab && matchesCollection && matchesRingtoneCategory && matchesSearch
        }.map { item ->
            val playback = ringtoneViewModel.playbackState.value
            val active = playback?.contentId == item.id
            ContentCard(
                content = item,
                isFavorite = item.ref.toFavoriteKey() in favorites,
                isPlaying = active && playback?.isPlaying == true,
                isPreparing = active && playback?.isPreparing == true,
                playbackProgress = if (active) playback?.progress ?: 0 else 0
            )
        }

        if (tab == MainTab.RINGTONES) {
            ringtoneFeedAdapter?.submitCards(cards)
        } else {
            contentAdapter?.submitList(cards)
        }
        val empty = cards.isEmpty()
        binding.rvContent.isVisible = !empty
        binding.favoriteEmptyState.isVisible = empty
        if (empty) {
            (parentFragment as? HomeFragment)?.revealChromeIfActive(tab)
        }
        val emptyFavorites = tab == MainTab.FAVORITES && query.isBlank()
        binding.emptyTitle.setText(
            if (emptyFavorites) R.string.no_favorites_yet else R.string.home_no_results
        )
        binding.emptyDescription.setText(
            if (emptyFavorites) R.string.no_favorites_description
            else R.string.home_no_results_description
        )
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
        ringtoneFeedAdapter = null
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
            if (columns <= 1) {
                outRect.left = 0
                outRect.right = 0
            } else {
                outRect.left = if (position % columns == 1) gap / 2 else 0
                outRect.right = if (position % columns == 0) gap / 2 else 0
            }
            outRect.bottom = gap
        }
    }

    companion object {
        private const val ARG_TAB = "main_tab"
        private const val ARG_COLLECTION = "home_collection"
        private const val ARG_RINGTONE_CATEGORY = "ringtone_category"

        fun newInstance(
            tab: MainTab,
            collection: WallpaperCollection,
            ringtoneCategory: RingtoneCategory = RingtoneCategory.RINGTONES
        ) = HomeCollectionFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_TAB, tab.name)
                putString(ARG_COLLECTION, collection.name)
                putString(ARG_RINGTONE_CATEGORY, ringtoneCategory.name)
            }
        }
    }
}

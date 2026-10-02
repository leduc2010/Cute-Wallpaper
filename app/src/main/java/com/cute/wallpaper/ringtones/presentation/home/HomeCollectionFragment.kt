package com.cute.wallpaper.ringtones.presentation.home

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
import com.cute.wallpaper.ringtones.databinding.FragmentHomeCollectionBinding
import com.cute.wallpaper.ringtones.presentation.base.BaseFragment
import com.cute.wallpaper.ringtones.presentation.detail.WallpaperDetailViewModel
import com.cute.wallpaper.ringtones.presentation.home.demo.DemoArtwork
import com.cute.wallpaper.ringtones.presentation.home.demo.DemoCollection
import com.cute.wallpaper.ringtones.presentation.home.demo.DemoContentAdapter
import com.cute.wallpaper.ringtones.presentation.home.demo.DemoContentCard
import com.cute.wallpaper.ringtones.presentation.main.MainTab
import com.cute.wallpaper.ringtones.presentation.main.hasCollections
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HomeCollectionFragment : BaseFragment<FragmentHomeCollectionBinding>() {
    @Inject lateinit var fakeContentDataSource: FakeContentDataSource

    private val viewModel: HomeViewModel by viewModels(ownerProducer = { requireParentFragment() })
    private val tab: MainTab by lazy {
        MainTab.entries.firstOrNull { it.name == arguments?.getString(ARG_TAB) } ?: MainTab.WALLPAPERS
    }
    private val collection: DemoCollection by lazy {
        DemoCollection.entries.firstOrNull { it.name == arguments?.getString(ARG_COLLECTION) }
            ?.canonical() ?: DemoCollection.WALLPAPER
    }
    private var contentAdapter: DemoContentAdapter? = null
    private var infoDialog: AlertDialog? = null

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentHomeCollectionBinding {
        return FragmentHomeCollectionBinding.inflate(inflater, container, false)
    }

    override fun initView() {
        val artwork = DemoArtwork(resources, fakeContentDataSource.artworkRegions)
        contentAdapter = DemoContentAdapter(artwork, { content, favorite ->
            viewModel.setFavorite(content.ref, favorite)
        }, ::openDetail).apply {
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
        binding.rvContent.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = contentAdapter
            itemAnimator = null
            addItemDecoration(GridSpacing(dp(14)))
        }
    }

    override fun observeData() {
        viewModel.contentItems.observe(viewLifecycleOwner) { renderContent() }
        viewModel.searchQuery.observe(viewLifecycleOwner) { renderContent() }
        viewModel.favoriteKeys.observe(viewLifecycleOwner) { renderContent() }
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
            val matchesSearch = item.matches(query)
            matchesTab && matchesCollection && matchesSearch
        }.map { DemoContentCard(it, it.ref.toFavoriteKey() in favorites) }

        contentAdapter?.submitList(cards)
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
            outRect.left = if (position % 2 == 1) gap / 2 else 0
            outRect.right = if (position % 2 == 0) gap / 2 else 0
            outRect.bottom = gap
        }
    }

    companion object {
        private const val ARG_TAB = "main_tab"
        private const val ARG_COLLECTION = "home_collection"

        fun newInstance(tab: MainTab, collection: DemoCollection) = HomeCollectionFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_TAB, tab.name)
                putString(ARG_COLLECTION, collection.name)
            }
        }
    }
}

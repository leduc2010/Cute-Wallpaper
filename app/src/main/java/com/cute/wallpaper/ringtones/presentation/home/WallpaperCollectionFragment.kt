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
import androidx.recyclerview.widget.RecyclerView
import com.cute.wallpaper.ringtones.R
import com.cute.wallpaper.ringtones.databinding.FragmentHomeCollectionBinding
import com.cute.wallpaper.ringtones.domain.model.ContentType
import com.cute.wallpaper.ringtones.presentation.base.BaseFragment
import com.cute.wallpaper.ringtones.presentation.detail.WallpaperDetailViewModel
import com.cute.wallpaper.ringtones.presentation.main.MainFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WallpaperCollectionFragment : BaseFragment<FragmentHomeCollectionBinding>() {
    private val viewModel: HomeViewModel by viewModels(ownerProducer = { requireParentFragment() })
    private val collection: WallpaperCollection by lazy {
        WallpaperCollection.entries.firstOrNull {
            it.name == arguments?.getString(ARG_COLLECTION)
        } ?: WallpaperCollection.WALLPAPER
    }
    private var contentAdapter: ContentAdapter? = null

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentHomeCollectionBinding.inflate(inflater, container, false)

    override fun initView() {
        contentAdapter = ContentAdapter(
            onFavorite = { content, favorite -> viewModel.setFavorite(content.ref, favorite) },
            onPreview = ::openDetail
        ).apply {
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
        binding.rvContent.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = contentAdapter
            itemAnimator = null
            addItemDecoration(GridSpacing(dp(16)))
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
            item.type == ContentType.WALLPAPER && item.collection == collection && item.matches(query)
        }.map { item ->
            ContentCard(content = item, isFavorite = item.ref.toFavoriteKey() in favorites)
        }
        contentAdapter?.submitList(cards)
        val empty = cards.isEmpty()
        binding.rvContent.isVisible = !empty
        binding.favoriteEmptyState.isVisible = empty
        if (empty) (parentFragment?.parentFragment as? MainFragment)?.revealChrome()
        binding.emptyTitle.setText(R.string.home_no_results)
        binding.emptyDescription.setText(R.string.home_no_results_description)
    }

    private fun openDetail(content: HomeContentUiModel) {
        if (content.contentUrl.isNullOrBlank()) return
        navViewModel.navigate(
            R.id.wallpaperDetailFragment,
            Bundle().apply {
                putString(WallpaperDetailViewModel.ARG_CONTENT_ID, content.id)
                putString(WallpaperDetailViewModel.ARG_CONTENT_TYPE, content.type.name)
            }
        )
    }

    override fun onDestroyView() {
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
        private const val ARG_COLLECTION = "home_collection"

        fun newInstance(collection: WallpaperCollection) = WallpaperCollectionFragment().apply {
            arguments = Bundle().apply { putString(ARG_COLLECTION, collection.name) }
        }
    }
}

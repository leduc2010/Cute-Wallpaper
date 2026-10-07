package com.cute.wallpaper.ringtones.presentation.favorites

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cute.wallpaper.ringtones.R
import com.cute.wallpaper.ringtones.databinding.FragmentTabFavoritesBinding
import com.cute.wallpaper.ringtones.databinding.ItemCategoryTabBinding
import com.cute.wallpaper.ringtones.domain.model.ContentType
import com.cute.wallpaper.ringtones.domain.model.RingtoneTarget
import com.cute.wallpaper.ringtones.presentation.base.BaseFragment
import com.cute.wallpaper.ringtones.presentation.detail.VideoWallpaperDetailViewModel
import com.cute.wallpaper.ringtones.presentation.detail.WallpaperDetailViewModel
import com.cute.wallpaper.ringtones.presentation.home.ContentCard
import com.cute.wallpaper.ringtones.presentation.home.HomeContentUiModel
import com.cute.wallpaper.ringtones.presentation.main.MainFragment
import com.cute.wallpaper.ringtones.presentation.main.MainTab
import com.cute.wallpaper.ringtones.presentation.main.MainViewModel
import com.cute.wallpaper.ringtones.presentation.quotes.QuoteActions
import com.cute.wallpaper.ringtones.presentation.quotes.QuoteAdapter
import com.cute.wallpaper.ringtones.presentation.quotes.QuoteCard
import com.cute.wallpaper.ringtones.presentation.ringtone.RingtoneActionEvent
import com.cute.wallpaper.ringtones.presentation.ringtone.RingtoneTargetBottomSheet
import com.cute.wallpaper.ringtones.presentation.ringtone.RingtoneViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TabFavoritesFragment : BaseFragment<FragmentTabFavoritesBinding>() {
    private val viewModel: FavoritesViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels(ownerProducer = { requireParentFragment() })
    private val ringtoneViewModel: RingtoneViewModel by viewModels()
    private var contentAdapter: FavoritesContentAdapter? = null
    private var quoteAdapter: QuoteAdapter? = null
    private var renderedCategory: FavoriteCategory? = null
    private var renderedCategories = emptyList<FavoriteCategory>()
    private val categoryViews = mutableMapOf<FavoriteCategory, ItemCategoryTabBinding>()

    private val writeSettings = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        ringtoneViewModel.retryPendingSet()
    }
    private val storagePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) ringtoneViewModel.retryPendingSet() else showToast(R.string.ringtone_storage_permission_required)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) = FragmentTabFavoritesBinding.inflate(inflater, container, false)

    override fun initView() {
        contentAdapter = FavoritesContentAdapter(
            onFavorite = { item, favorite -> viewModel.setFavorite(item.ref, favorite) },
            onOpen = ::openContent,
            onSetRingtone = ::showRingtoneTarget
        ).apply { stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY }
        quoteAdapter = QuoteAdapter(
            onFavorite = { item, favorite -> viewModel.setFavorite(item.ref, favorite) },
            onOpen = ::openContent,
            onCopy = { QuoteActions.copy(requireContext(), it) },
            onShare = { QuoteActions.shareText(requireContext(), it) },
            showAttribution = true
        ).apply { stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY }
        binding.rvContent.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            itemAnimator = null
            addItemDecoration(Spacing(dp(12)))
        }
        childFragmentManager.setFragmentResultListener(RingtoneTargetBottomSheet.REQUEST_KEY, viewLifecycleOwner) { _, result ->
            val id = result.getString(RingtoneTargetBottomSheet.RESULT_CONTENT_ID).orEmpty()
            val target = RingtoneTarget.entries.firstOrNull { it.name == result.getString(RingtoneTargetBottomSheet.RESULT_TARGET) }
                ?: return@setFragmentResultListener
            ringtoneViewModel.requestSet(id, target)
        }
    }

    override fun initListener() {
        binding.etSearch.doAfterTextChanged { viewModel.updateQuery(it?.toString().orEmpty()) }
        binding.etSearch.setOnEditorActionListener { _, action, _ ->
            if (action != EditorInfo.IME_ACTION_SEARCH) false else {
                binding.etSearch.clearFocus()
                (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
                true
            }
        }
        binding.clearSearch.setOnClickListener { viewModel.updateQuery("") }
    }

    override fun observeData() {
        viewModel.favoriteKeys.observe(viewLifecycleOwner) { renderContent() }
        viewModel.selectedCategory.observe(viewLifecycleOwner) { renderContent() }
        viewModel.searchQuery.observe(viewLifecycleOwner) { query ->
            if (binding.etSearch.text?.toString() != query) binding.etSearch.setText(query)
            renderContent()
        }
        viewModel.searchVisible.observe(viewLifecycleOwner) { binding.etSearch.isVisible = it }
        ringtoneViewModel.playbackState.observe(viewLifecycleOwner) { renderContent() }
        viewModel.durationByContentId.observe(viewLifecycleOwner) { renderContent() }
        ringtoneViewModel.actionEvent.observe(viewLifecycleOwner) { event -> event.getContentIfNotHandled()?.let(::handleRingtoneAction) }
        mainViewModel.bottomContentPadding.observe(viewLifecycleOwner) { padding ->
            binding.rvContent.updatePadding(bottom = padding + dp(16))
        }
        mainViewModel.selectedTab.observe(viewLifecycleOwner) { tab ->
            if (tab != MainTab.FAVORITES) ringtoneViewModel.stopPreview()
        }
    }

    private fun renderContent() {
        val favorites = viewModel.favoriteKeys.value.orEmpty()
        val saved = viewModel.contents.filter { it.ref.toFavoriteKey() in favorites }
        val categories = FavoriteCategory.entries
        val category = FavoriteCategory.entries.firstOrNull { it.name == viewModel.selectedCategory.value && it in categories }
            ?: FavoriteCategory.WALLPAPER
        if (viewModel.selectedCategory.value != category.name) {
            viewModel.selectCategory(category)
            return
        }
        renderCategories(categories, category)
        if (renderedCategory != category) {
            renderedCategory = category
            ringtoneViewModel.stopPreview()
            (binding.rvContent.layoutManager as GridLayoutManager).spanCount = category.columns
            binding.rvContent.adapter = when (category) {
                FavoriteCategory.QUOTES -> quoteAdapter
                else -> contentAdapter
            }
            binding.rvContent.scrollToPosition(0)
        }
        val query = viewModel.searchQuery.value.orEmpty().trim()
        val filtered = saved.filter { category.matches(it) && it.matches(query) }
        if (category == FavoriteCategory.VIDEO) {
            filtered.forEach { item ->
                viewModel.ensureVideoDuration(item.id, item.contentUrl)
            }
        }
        val playback = ringtoneViewModel.playbackState.value
        val durations = viewModel.durationByContentId.value.orEmpty()
        val cards = filtered.map { item ->
            val active = playback?.contentId == item.id
            ContentCard(
                content = item,
                isFavorite = true,
                isPlaying = active && playback?.isPlaying == true,
                isPreparing = active && playback?.isPreparing == true,
                playbackProgress = if (active) playback?.progress ?: 0 else 0,
                currentPositionMs = if (active) playback?.currentPositionMs ?: 0L else 0L,
                durationMs = when {
                    item.type == ContentType.VIDEO_WALLPAPER -> durations[item.id] ?: 0L
                    active -> playback?.durationMs ?: 0L
                    else -> 0L
                }
            )
        }
        when (category) {
            FavoriteCategory.QUOTES -> quoteAdapter?.submitList(filtered.map { QuoteCard(it, true) })
            else -> contentAdapter?.submitList(cards)
        }
        binding.rvContent.isVisible = filtered.isNotEmpty()
        binding.emptyState.isVisible = filtered.isEmpty()
        binding.emptyTitle.setText(if (query.isBlank()) R.string.no_favorites_yet else R.string.home_no_results)
        binding.emptyDescription.setText(if (query.isBlank()) R.string.no_favorites_description else R.string.home_no_results_description)
        if (filtered.isEmpty() && mainViewModel.selectedTab.value == MainTab.FAVORITES) {
            (parentFragment as? MainFragment)?.revealChrome(animate = false)
        }
    }

    private fun renderCategories(categories: List<FavoriteCategory>, selected: FavoriteCategory) {
        if (categories != renderedCategories) {
            binding.categories.removeAllViews()
            categoryViews.clear()
            categories.forEachIndexed { index, category ->
                val item = ItemCategoryTabBinding.inflate(
                    layoutInflater,
                    binding.categories,
                    false
                )
                item.tabIcon.setImageResource(category.iconRes)
                item.tabTitle.setText(category.titleRes)
                item.root.apply {
                    contentDescription = getString(category.titleRes)
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        dp(40)
                    ).apply {
                        marginStart = if (index == 0) 0 else dp(10)
                    }
                    setOnClickListener { viewModel.selectCategory(category) }
                }
                binding.categories.addView(item.root)
                categoryViews[category] = item
            }
            renderedCategories = categories
        }
        categoryViews.forEach { (category, item) ->
            val isSelected = category == selected
            item.root.isSelected = isSelected
            item.root.setPaddingRelative(0, 0, dp(if (isSelected) 12 else 0), 0)
            item.tabTitle.isVisible = isSelected
        }
    }

    fun openSearch() {
        viewModel.searchVisible.value = true
        binding.etSearch.requestFocus()
        (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun openContent(item: HomeContentUiModel) {
        if (item.type != ContentType.QUOTE && item.contentUrl.isNullOrBlank()) return
        val args = Bundle()
        val destination = when (item.type) {
            ContentType.WALLPAPER -> R.id.wallpaperDetailFragment.also {
                args.putString(WallpaperDetailViewModel.ARG_CONTENT_ID, item.id)
                args.putString(WallpaperDetailViewModel.ARG_CONTENT_TYPE, item.type.name)
            }
            ContentType.VIDEO_WALLPAPER -> R.id.videoWallpaperDetailFragment.also { args.putString(VideoWallpaperDetailViewModel.ARG_CONTENT_ID, item.id) }
            ContentType.PROFILE_PICTURE -> return
            ContentType.QUOTE -> R.id.quoteDetailFragment.also { args.putString("quote_content_id", item.id) }
            ContentType.RINGTONE -> { ringtoneViewModel.togglePreview(item.id); return }
        }
        navViewModel.navigate(destination, args)
    }

    private fun showRingtoneTarget(item: HomeContentUiModel) {
        if (childFragmentManager.findFragmentByTag(RingtoneTargetBottomSheet.TAG) != null) return
        RingtoneTargetBottomSheet.newInstance(item.id).show(childFragmentManager, RingtoneTargetBottomSheet.TAG)
    }

    private fun handleRingtoneAction(event: RingtoneActionEvent) {
        when (event) {
            RingtoneActionEvent.RequestWriteSettings -> {
                showToast(R.string.ringtone_write_settings_required)
                writeSettings.launch(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${requireContext().packageName}")))
            }
            RingtoneActionEvent.RequestStoragePermission -> storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            is RingtoneActionEvent.SetSuccess -> showToast(R.string.ringtone_set_success)
            RingtoneActionEvent.SetFailed -> showToast(R.string.ringtone_set_failed)
            RingtoneActionEvent.PreviewFailed -> showToast(R.string.ringtone_preview_failed)
        }
    }

    override fun onPause() { ringtoneViewModel.stopPreview(); super.onPause() }

    override fun onDestroyView() {
        binding.rvContent.adapter = null
        contentAdapter = null
        quoteAdapter = null
        categoryViews.clear()
        renderedCategories = emptyList()
        renderedCategory = null
        super.onDestroyView()
    }

    private fun showToast(message: Int) = Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private class Spacing(private val gap: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            val position = parent.getChildAdapterPosition(view)
            if (position == RecyclerView.NO_POSITION) return
            val columns = (parent.layoutManager as GridLayoutManager).spanCount
            val column = position % columns
            outRect.left = column * gap / columns
            outRect.right = gap - (column + 1) * gap / columns
            outRect.bottom = gap
        }
    }
}

package com.cute.wallpaper.ringtones.presentation.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cute.wallpaper.ringtones.R
import com.cute.wallpaper.ringtones.databinding.FragmentHomeCollectionBinding
import com.cute.wallpaper.ringtones.domain.model.ContentType
import com.cute.wallpaper.ringtones.domain.model.RingtoneTarget
import com.cute.wallpaper.ringtones.presentation.base.BaseFragment
import com.cute.wallpaper.ringtones.presentation.main.MainFragment
import com.cute.wallpaper.ringtones.presentation.ringtone.RingtoneTargetBottomSheet
import com.cute.wallpaper.ringtones.presentation.ringtone.RingtoneViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RingtoneCategoryFragment : BaseFragment<FragmentHomeCollectionBinding>() {
    private val viewModel: HomeViewModel by viewModels(ownerProducer = { requireParentFragment() })
    private val ringtoneViewModel: RingtoneViewModel by viewModels(ownerProducer = { requireParentFragment() })
    private val category: RingtoneCategory by lazy {
        RingtoneCategory.entries.firstOrNull {
            it.name == arguments?.getString(ARG_CATEGORY)
        } ?: RingtoneCategory.RINGTONES
    }
    private var feedAdapter: RingtoneFeedAdapter? = null

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentHomeCollectionBinding.inflate(inflater, container, false)

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
        feedAdapter = RingtoneFeedAdapter(
            onFavorite = { content, favorite -> viewModel.setFavorite(content.ref, favorite) },
            onPreview = { ringtoneViewModel.togglePreview(it.id) },
            onSet = ::showRingtoneTarget
        ).apply {
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
        binding.rvContent.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = feedAdapter
            itemAnimator = null
        }
    }

    override fun observeData() {
        viewModel.contentItems.observe(viewLifecycleOwner) { renderContent() }
        viewModel.searchQuery.observe(viewLifecycleOwner) { renderContent() }
        viewModel.favoriteKeys.observe(viewLifecycleOwner) { renderContent() }
        ringtoneViewModel.playbackState.observe(viewLifecycleOwner) { renderContent() }
        ringtoneViewModel.durationByContentId.observe(viewLifecycleOwner) { renderContent() }
        viewModel.bottomContentPadding.observe(viewLifecycleOwner) { padding ->
            binding.rvContent.updatePadding(bottom = padding + dp(16))
        }
    }

    private fun renderContent() {
        val favorites = viewModel.favoriteKeys.value.orEmpty()
        val query = viewModel.searchQuery.value.orEmpty().trim()
        val playback = ringtoneViewModel.playbackState.value
        val durations = ringtoneViewModel.durationByContentId.value.orEmpty()
        val items = viewModel.contentItems.value.orEmpty().filter { item ->
            item.type == ContentType.RINGTONE &&
                item.category == category.remoteKey &&
                item.matches(query)
        }
        items.firstOrNull()?.let { ringtoneViewModel.ensureDuration(it.id) }

        val cards = items.map { item ->
            val active = playback?.contentId == item.id
            val playbackDuration = playback?.durationMs?.takeIf { active && it > 0 }
            ContentCard(
                content = item,
                isFavorite = item.ref.toFavoriteKey() in favorites,
                isPlaying = active && playback?.isPlaying == true,
                isPreparing = active && playback?.isPreparing == true,
                playbackProgress = if (active) playback?.progress ?: 0 else 0,
                currentPositionMs = if (active) playback?.currentPositionMs ?: 0L else 0L,
                durationMs = playbackDuration ?: durations[item.id] ?: 0L
            )
        }
        feedAdapter?.submitCards(cards)
        val empty = cards.isEmpty()
        binding.rvContent.isVisible = !empty
        binding.favoriteEmptyState.isVisible = empty
        if (empty) (parentFragment?.parentFragment as? MainFragment)?.revealChrome()
        binding.emptyTitle.setText(R.string.home_no_results)
        binding.emptyDescription.setText(R.string.home_no_results_description)
    }

    private fun showRingtoneTarget(content: HomeContentUiModel) {
        if (childFragmentManager.findFragmentByTag(RingtoneTargetBottomSheet.TAG) != null) return
        RingtoneTargetBottomSheet.newInstance(content.id).show(
            childFragmentManager,
            RingtoneTargetBottomSheet.TAG
        )
    }

    override fun onDestroyView() {
        binding.rvContent.adapter = null
        feedAdapter = null
        super.onDestroyView()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val ARG_CATEGORY = "ringtone_category"

        fun newInstance(category: RingtoneCategory) = RingtoneCategoryFragment().apply {
            arguments = Bundle().apply { putString(ARG_CATEGORY, category.name) }
        }
    }
}

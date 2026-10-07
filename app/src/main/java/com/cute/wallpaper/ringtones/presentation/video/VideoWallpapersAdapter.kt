package com.cute.wallpaper.ringtones.presentation.video

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.cute.wallpaper.ringtones.R
import com.cute.wallpaper.ringtones.databinding.ItemVideoWallpaperFeaturedBinding
import com.cute.wallpaper.ringtones.databinding.ItemVideoWallpaperGridBinding
import com.cute.wallpaper.ringtones.databinding.ItemVideoWallpaperHotSectionBinding
import com.cute.wallpaper.ringtones.databinding.ItemVideoWallpaperSectionHeaderBinding
import com.cute.wallpaper.ringtones.presentation.home.ContentCard
import com.cute.wallpaper.ringtones.presentation.home.HomeContentUiModel

internal sealed interface VideoWallpaperRow {
    val key: String

    data class Featured(val cards: List<ContentCard>) : VideoWallpaperRow {
        override val key: String = "featured:" + cards.joinToString("|") { it.content.id }
    }

    data class Hot(val cards: List<ContentCard>) : VideoWallpaperRow {
        override val key: String = "hot:" + cards.joinToString("|") { it.content.id }
    }

    data object ForYouHeader : VideoWallpaperRow {
        override val key: String = "for-you-header"
    }

    data class ForYou(val card: ContentCard) : VideoWallpaperRow {
        override val key: String = "for-you:" + card.content.id
    }
}

internal class VideoWallpapersAdapter(
    private val onFavorite: (HomeContentUiModel, Boolean) -> Unit,
    private val onOpen: (HomeContentUiModel) -> Unit
) : ListAdapter<VideoWallpaperRow, RecyclerView.ViewHolder>(Diff) {

    fun submitCards(cards: List<ContentCard>) {
        if (cards.isEmpty()) {
            submitList(emptyList())
            return
        }

        val ordered = cards.sortedBy { it.content.rank }
        val featuredCards = ordered.take(VIDEO_FEATURED_ITEM_COUNT)
        val afterFeatured = ordered.drop(featuredCards.size)
        val hotCards = afterFeatured.take(VIDEO_HOT_ITEM_COUNT)
            .takeIf { it.size == VIDEO_HOT_ITEM_COUNT }
            .orEmpty()
        val forYouStart = featuredCards.size + hotCards.size

        val rows = buildList {
            if (featuredCards.isNotEmpty()) {
                add(VideoWallpaperRow.Featured(featuredCards))
            }
            if (hotCards.isNotEmpty()) {
                add(VideoWallpaperRow.Hot(hotCards))
            }

            val forYou = ordered.drop(forYouStart)
            if (forYou.isNotEmpty()) {
                add(VideoWallpaperRow.ForYouHeader)
                forYou.forEach { add(VideoWallpaperRow.ForYou(it)) }
            }
        }
        submitList(rows)
    }

    fun spanSize(position: Int): Int =
        if (getItemViewType(position) == VIEW_FOR_YOU) 1 else GRID_SPAN_COUNT

    fun isForYou(position: Int): Boolean =
        position in 0 until itemCount && getItemViewType(position) == VIEW_FOR_YOU

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is VideoWallpaperRow.Featured -> VIEW_FEATURED
        is VideoWallpaperRow.Hot -> VIEW_HOT
        VideoWallpaperRow.ForYouHeader -> VIEW_HEADER
        is VideoWallpaperRow.ForYou -> VIEW_FOR_YOU
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_FEATURED -> FeaturedHolder(
                ItemVideoWallpaperFeaturedBinding.inflate(inflater, parent, false)
            )

            VIEW_HOT -> HotHolder(
                ItemVideoWallpaperHotSectionBinding.inflate(inflater, parent, false)
            )

            VIEW_HEADER -> HeaderHolder(
                ItemVideoWallpaperSectionHeaderBinding.inflate(inflater, parent, false)
            )

            else -> GridHolder(
                ItemVideoWallpaperGridBinding.inflate(inflater, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is VideoWallpaperRow.Featured -> (holder as FeaturedHolder).bind(row.cards)
            is VideoWallpaperRow.Hot -> (holder as HotHolder).bind(row.cards)
            VideoWallpaperRow.ForYouHeader -> Unit
            is VideoWallpaperRow.ForYou -> (holder as GridHolder).bind(row.card)
        }
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        (holder as? FeaturedHolder)?.onAttached()
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        (holder as? FeaturedHolder)?.onDetached()
        super.onViewDetachedFromWindow(holder)
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        (holder as? FeaturedHolder)?.onDetached()
        super.onViewRecycled(holder)
    }

    private fun bindImage(image: ImageView, item: HomeContentUiModel) {
        Glide.with(image)
            .load(item.thumbnailUrl ?: item.contentUrl)
            .centerCrop()
            .into(image)
    }

    private fun bindFavorite(button: ImageView, card: ContentCard) {
        button.isSelected = card.isFavorite
        button.setOnClickListener {
            onFavorite(card.content, !card.isFavorite)
        }
    }

    private inner class FeaturedHolder(
        private val binding: ItemVideoWallpaperFeaturedBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val pagerAdapter = VideoFeaturedPagerAdapter(onFavorite, onOpen)
        private var attached = false
        private var dragging = false

        private val autoScroll = object : Runnable {
            override fun run() {
                if (!attached || dragging || pagerAdapter.itemCount <= 1) return
                val next = (binding.featuredPager.currentItem + 1) % pagerAdapter.itemCount
                binding.featuredPager.setCurrentItem(next, true)
                scheduleAutoScroll()
            }
        }

        private val pageCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                renderIndicator(position)
            }

            override fun onPageScrollStateChanged(state: Int) {
                dragging = state == ViewPager2.SCROLL_STATE_DRAGGING
                if (dragging) {
                    stopAutoScroll()
                } else if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    scheduleAutoScroll()
                }
            }
        }

        init {
            binding.featuredPager.adapter = pagerAdapter
            binding.featuredPager.offscreenPageLimit = 1
            binding.featuredPager.registerOnPageChangeCallback(pageCallback)
        }

        fun bind(cards: List<ContentCard>) {
            val currentId = pagerAdapter.currentList
                .getOrNull(binding.featuredPager.currentItem)
                ?.content
                ?.id

            pagerAdapter.submitList(cards) {
                val currentIndex = binding.featuredPager.currentItem
                val target = currentId
                    ?.let { id -> cards.indexOfFirst { it.content.id == id } }
                    ?.takeIf { it >= 0 }
                    ?: currentIndex.coerceIn(0, cards.lastIndex.coerceAtLeast(0))

                if (cards.isNotEmpty() && currentIndex != target) {
                    binding.featuredPager.setCurrentItem(target, false)
                }
                renderIndicator(binding.featuredPager.currentItem)
                scheduleAutoScroll()
            }
        }

        fun onAttached() {
            attached = true
            scheduleAutoScroll()
        }

        fun onDetached() {
            attached = false
            stopAutoScroll()
        }

        private fun scheduleAutoScroll() {
            stopAutoScroll()
            if (attached && !dragging && pagerAdapter.itemCount > 1) {
                binding.featuredPager.postDelayed(autoScroll, VIDEO_FEATURED_AUTO_SCROLL_MS)
            }
        }

        private fun stopAutoScroll() {
            binding.featuredPager.removeCallbacks(autoScroll)
        }

        private fun renderIndicator(selectedPosition: Int) {
            val count = pagerAdapter.itemCount
            binding.featuredIndicator.removeAllViews()
            binding.featuredIndicator.isVisible = count > 1
            if (count <= 1) return

            repeat(count) { index ->
                val dot = LayoutInflater.from(binding.root.context)
                    .inflate(
                        R.layout.item_video_featured_indicator_dot,
                        binding.featuredIndicator,
                        false
                    )
                dot.isSelected = index == selectedPosition
                binding.featuredIndicator.addView(dot)
            }
        }
    }

    private inner class HotHolder(
        private val binding: ItemVideoWallpaperHotSectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(cards: List<ContentCard>) {
            bindHotCard(
                binding.hotLeft,
                binding.hotLeftImage,
                binding.hotLeftFavorite,
                binding.hotLeftPlay,
                cards.getOrNull(0)
            )
            bindHotCard(
                binding.hotMiddleTop,
                binding.hotMiddleTopImage,
                binding.hotMiddleTopFavorite,
                binding.hotMiddleTopPlay,
                cards.getOrNull(1)
            )
            bindHotCard(
                binding.hotMiddleBottom,
                binding.hotMiddleBottomImage,
                binding.hotMiddleBottomFavorite,
                binding.hotMiddleBottomPlay,
                cards.getOrNull(2)
            )
            bindHotCard(
                binding.hotRight,
                binding.hotRightImage,
                binding.hotRightFavorite,
                binding.hotRightPlay,
                cards.getOrNull(3)
            )
        }

        private fun bindHotCard(
            container: View,
            image: ImageView,
            favorite: ImageView,
            play: View,
            card: ContentCard?
        ) {
            container.isVisible = card != null
            if (card == null) return
            bindImage(image, card.content)
            container.setOnClickListener { onOpen(card.content) }
            play.setOnClickListener { onOpen(card.content) }
            bindFavorite(favorite, card)
        }
    }

    private inner class GridHolder(
        private val binding: ItemVideoWallpaperGridBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(card: ContentCard) {
            bindImage(binding.artwork, card.content)
            binding.root.setOnClickListener { onOpen(card.content) }
            binding.playButton.setOnClickListener { onOpen(card.content) }
            bindFavorite(binding.favoriteButton, card)
        }
    }

    private class HeaderHolder(
        binding: ItemVideoWallpaperSectionHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root)

    private object Diff : DiffUtil.ItemCallback<VideoWallpaperRow>() {
        override fun areItemsTheSame(
            oldItem: VideoWallpaperRow,
            newItem: VideoWallpaperRow
        ): Boolean = oldItem.key == newItem.key

        override fun areContentsTheSame(
            oldItem: VideoWallpaperRow,
            newItem: VideoWallpaperRow
        ): Boolean = oldItem == newItem
    }

    companion object {
        const val GRID_SPAN_COUNT = 3
        private const val VIEW_FEATURED = 0
        private const val VIEW_HOT = 1
        private const val VIEW_HEADER = 2
        private const val VIEW_FOR_YOU = 3
    }
}

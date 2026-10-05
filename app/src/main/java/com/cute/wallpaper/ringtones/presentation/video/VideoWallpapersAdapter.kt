package com.cute.wallpaper.ringtones.presentation.video

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
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

    data class Featured(val card: ContentCard) : VideoWallpaperRow {
        override val key: String = "featured:${card.content.id}"
    }

    data class Hot(val cards: List<ContentCard>) : VideoWallpaperRow {
        override val key: String = "hot:" + cards.joinToString("|") { it.content.id }
    }

    data object ForYouHeader : VideoWallpaperRow {
        override val key: String = "for-you-header"
    }

    data class ForYou(val card: ContentCard) : VideoWallpaperRow {
        override val key: String = "for-you:${card.content.id}"
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
        val hotCards = ordered.drop(1).take(HOT_ITEM_COUNT)
            .takeIf { it.size == HOT_ITEM_COUNT }
            .orEmpty()
        val forYouStart = if (hotCards.isEmpty()) 1 else 1 + HOT_ITEM_COUNT
        val rows = buildList {
            add(VideoWallpaperRow.Featured(ordered.first()))
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

    fun spanSize(position: Int): Int {
        return if (getItemViewType(position) == VIEW_FOR_YOU) 1 else GRID_SPAN_COUNT
    }

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
            is VideoWallpaperRow.Featured -> (holder as FeaturedHolder).bind(row.card)
            is VideoWallpaperRow.Hot -> (holder as HotHolder).bind(row.cards)
            VideoWallpaperRow.ForYouHeader -> Unit
            is VideoWallpaperRow.ForYou -> (holder as GridHolder).bind(row.card)
        }
    }

    private fun bindImage(image: ImageView, item: HomeContentUiModel) {
        image.contentDescription = item.title
        Glide.with(image)
            .load(item.thumbnailUrl ?: item.contentUrl)
            .centerCrop()
            .into(image)
    }

    private fun bindFavorite(button: ImageView, card: ContentCard) {
        button.setImageResource(
            if (card.isFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
        )
        button.contentDescription = button.context.getString(
            if (card.isFavorite) R.string.home_remove_favorite
            else R.string.home_add_favorite,
            card.content.title
        )
        button.setOnClickListener {
            onFavorite(card.content, !card.isFavorite)
        }
    }

    private inner class FeaturedHolder(
        private val binding: ItemVideoWallpaperFeaturedBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(card: ContentCard) {
            bindImage(binding.artwork, card.content)
            binding.title.text = card.content.title
            binding.root.setOnClickListener { onOpen(card.content) }
            binding.playButton.setOnClickListener { onOpen(card.content) }
            bindFavorite(binding.favoriteButton, card)
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
        private const val HOT_ITEM_COUNT = 4
        private const val VIEW_FEATURED = 0
        private const val VIEW_HOT = 1
        private const val VIEW_HEADER = 2
        private const val VIEW_FOR_YOU = 3
    }
}

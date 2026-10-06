package com.cute.wallpaper.ringtones.presentation.home

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cute.wallpaper.ringtones.R
import com.cute.wallpaper.ringtones.databinding.ItemRingtoneBinding
import com.cute.wallpaper.ringtones.databinding.ItemRingtoneFeaturedBinding
import com.cute.wallpaper.ringtones.databinding.ItemRingtoneSectionHeaderBinding

internal sealed interface RingtoneFeedRow {
    val key: String

    data class Featured(val card: ContentCard) : RingtoneFeedRow {
        override val key = "featured:${card.content.ref}"
    }

    data object SectionHeader : RingtoneFeedRow {
        override val key = "today-picks"
    }

    data class Item(val card: ContentCard, val backgroundRes: Int) : RingtoneFeedRow {
        override val key = "item:${card.content.ref}"
    }
}

internal class RingtoneFeedAdapter(
    private val onFavorite: (HomeContentUiModel, Boolean) -> Unit,
    private val onPreview: (HomeContentUiModel) -> Unit,
    private val onSet: (HomeContentUiModel) -> Unit
) : ListAdapter<RingtoneFeedRow, RecyclerView.ViewHolder>(Diff) {

    fun submitCards(cards: List<ContentCard>) {
        val rows = buildList {
            val featured = cards.firstOrNull() ?: return@buildList
            add(RingtoneFeedRow.Featured(featured))
            if (cards.size > 1) {
                add(RingtoneFeedRow.SectionHeader)
                cards.drop(1).forEach { card ->
                    val backgroundIndex = Math.floorMod(
                        card.content.id.hashCode(),
                        ringtoneBackgrounds.size
                    )
                    add(
                        RingtoneFeedRow.Item(
                            card = card,
                            backgroundRes = ringtoneBackgrounds[backgroundIndex]
                        )
                    )
                }
            }
        }
        submitList(rows)
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is RingtoneFeedRow.Featured -> VIEW_FEATURED
        RingtoneFeedRow.SectionHeader -> VIEW_HEADER
        is RingtoneFeedRow.Item -> VIEW_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_FEATURED -> FeaturedHolder(ItemRingtoneFeaturedBinding.inflate(inflater, parent, false))
            VIEW_HEADER -> HeaderHolder(ItemRingtoneSectionHeaderBinding.inflate(inflater, parent, false))
            else -> ItemHolder(ItemRingtoneBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is RingtoneFeedRow.Featured -> (holder as FeaturedHolder).bind(row.card)
            RingtoneFeedRow.SectionHeader -> Unit
            is RingtoneFeedRow.Item -> (holder as ItemHolder).bind(row)
        }
    }

    private fun bindFavorite(button: ImageView, card: ContentCard) {
        button.setImageResource(
            if (card.isFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
        )
        button.setOnClickListener { onFavorite(card.content, !card.isFavorite) }
    }

    private inner class FeaturedHolder(
        private val binding: ItemRingtoneFeaturedBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(card: ContentCard) {
            val item = card.content
            binding.contentTitle.text = item.title
            binding.contentSubtitle.text = item.tags
                .filterNot { it.equals("new", ignoreCase = true) }
                .take(3)
                .joinToString(" · ")
                .ifBlank { item.category.replaceFirstChar(Char::titlecase) }
            binding.playLoading.isVisible = card.isPreparing
            binding.playButton.isVisible = !card.isPreparing
            binding.playContainer.setBackgroundResource(
                if (card.isPlaying) R.drawable.bg_ringtone_play_active else R.drawable.bg_home_action
            )
            binding.playButton.setImageResource(
                if (card.isPlaying) R.drawable.ic_ringtone_pause else R.drawable.ic_ringtone_play
            )
            binding.playProgress.setProgressCompat(card.playbackProgress, false)
            binding.playButton.setOnClickListener { onPreview(item) }
            binding.root.setOnClickListener { onPreview(item) }
            binding.setButton.setOnClickListener { onSet(item) }
            bindFavorite(binding.favoriteButton, card)
        }
    }

    private inner class ItemHolder(
        private val binding: ItemRingtoneBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(row: RingtoneFeedRow.Item) {
            val card = row.card
            val item = card.content
            binding.root.setBackgroundResource(row.backgroundRes)
            binding.contentTitle.text = item.title
            binding.newBadge.isVisible = item.tags.any { it.equals("new", ignoreCase = true) }
            binding.playLoading.isVisible = card.isPreparing
            binding.playButton.isVisible = !card.isPreparing
            binding.playContainer.setBackgroundResource(
                if (card.isPlaying) R.drawable.bg_ringtone_play_active else R.drawable.bg_home_action
            )
            binding.playButton.setImageResource(
                if (card.isPlaying) R.drawable.ic_ringtone_pause else R.drawable.ic_ringtone_play
            )
            binding.playButton.setOnClickListener { onPreview(item) }
            binding.root.setOnClickListener { onPreview(item) }
            binding.setButton.setOnClickListener { onSet(item) }
            bindFavorite(binding.favoriteButton, card)
        }
    }

    private class HeaderHolder(
        binding: ItemRingtoneSectionHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root)

    private object Diff : DiffUtil.ItemCallback<RingtoneFeedRow>() {
        override fun areItemsTheSame(oldItem: RingtoneFeedRow, newItem: RingtoneFeedRow) =
            oldItem.key == newItem.key

        override fun areContentsTheSame(oldItem: RingtoneFeedRow, newItem: RingtoneFeedRow) =
            oldItem == newItem
    }

    private companion object {
        const val VIEW_FEATURED = 0
        const val VIEW_HEADER = 1
        const val VIEW_ITEM = 2

        val ringtoneBackgrounds = intArrayOf(
            R.drawable.bg_favorites_ringtone_pink,
            R.drawable.bg_favorites_ringtone_blue,
            R.drawable.bg_favorites_ringtone_orange,
            R.drawable.bg_favorites_ringtone_purple,
            R.drawable.bg_favorites_ringtone_rose,
            R.drawable.bg_favorites_ringtone_lilac,
            R.drawable.bg_favorites_ringtone_peach
        )
    }
}

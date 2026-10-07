package com.cute.wallpaper.ringtones.presentation.video

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cute.wallpaper.ringtones.databinding.ItemVideoWallpaperFeaturedPageBinding
import com.cute.wallpaper.ringtones.presentation.home.ContentCard
import com.cute.wallpaper.ringtones.presentation.home.HomeContentUiModel

internal class VideoFeaturedPagerAdapter(
    private val onFavorite: (HomeContentUiModel, Boolean) -> Unit,
    private val onOpen: (HomeContentUiModel) -> Unit
) : ListAdapter<ContentCard, VideoFeaturedPagerAdapter.Holder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(
            ItemVideoWallpaperFeaturedPageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class Holder(
        private val binding: ItemVideoWallpaperFeaturedPageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(card: ContentCard) {
            Glide.with(binding.artwork)
                .load(card.content.thumbnailUrl ?: card.content.contentUrl)
                .centerCrop()
                .into(binding.artwork)

            binding.title.text = card.content.title
            binding.meta.text = featuredMeta(card)
            binding.favoriteButton.isSelected = card.isFavorite
            binding.favoriteButton.setOnClickListener {
                onFavorite(card.content, !card.isFavorite)
            }
            binding.root.setOnClickListener { onOpen(card.content) }
            binding.playButton.setOnClickListener { onOpen(card.content) }
        }
    }

    private fun featuredMeta(card: ContentCard): String {
        val duration = formatDuration(card.durationMs)
        val quality = card.qualityLabel
            ?: card.content.tags.firstOrNull { tag -> QUALITY_REGEX.matches(tag.trim()) }

        return buildString {
            append(duration)
            if (!quality.isNullOrBlank()) {
                append("  |  ✨ ")
                append(quality.uppercase())
            }
        }
    }

    private fun formatDuration(durationMs: Long): String {
        if (durationMs <= 0L) return "--:--"
        val totalSeconds = durationMs / 1_000L
        return "%02d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
    }

    private object Diff : DiffUtil.ItemCallback<ContentCard>() {
        override fun areItemsTheSame(oldItem: ContentCard, newItem: ContentCard): Boolean =
            oldItem.content.id == newItem.content.id

        override fun areContentsTheSame(oldItem: ContentCard, newItem: ContentCard): Boolean =
            oldItem == newItem
    }

    private companion object {
        val QUALITY_REGEX = Regex("(?i)^(4k|2k|8k|uhd|fhd|hd)$")
    }
}

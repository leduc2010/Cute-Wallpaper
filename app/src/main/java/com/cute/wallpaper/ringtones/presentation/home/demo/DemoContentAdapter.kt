package com.cute.wallpaper.ringtones.presentation.home.demo

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cute.wallpaper.ringtones.R
import com.cute.wallpaper.ringtones.core.model.ContentType
import com.cute.wallpaper.ringtones.databinding.ItemDemoTextContentBinding
import com.cute.wallpaper.ringtones.databinding.ItemDemoWallpaperBinding
import com.cute.wallpaper.ringtones.databinding.ItemSearchWallpaperBinding
import com.cute.wallpaper.ringtones.presentation.home.HomeContentUiModel

data class DemoContentCard(val content: HomeContentUiModel, val isFavorite: Boolean)

class DemoContentAdapter(
    private val artwork: DemoArtwork,
    private val onFavorite: (HomeContentUiModel, Boolean) -> Unit,
    private val onPreview: (HomeContentUiModel) -> Unit,
    private val compactArtwork: Boolean = false
) : ListAdapter<DemoContentCard, RecyclerView.ViewHolder>(DiffCallback) {
    override fun getItemViewType(position: Int): Int = when {
        getItem(position).content.artworkIndex == null -> VIEW_TYPE_TEXT
        compactArtwork -> VIEW_TYPE_COMPACT_ARTWORK
        else -> VIEW_TYPE_ARTWORK
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_ARTWORK -> ArtworkHolder(ItemDemoWallpaperBinding.inflate(inflater, parent, false))
            VIEW_TYPE_COMPACT_ARTWORK -> CompactArtworkHolder(
                ItemSearchWallpaperBinding.inflate(inflater, parent, false)
            )
            else -> TextHolder(ItemDemoTextContentBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val card = getItem(position)
        when (holder) {
            is ArtworkHolder -> holder.bind(card)
            is CompactArtworkHolder -> holder.bind(card)
            is TextHolder -> holder.bind(card)
        }
    }

    private fun bindFavorite(button: ImageView, card: DemoContentCard) {
        button.isSelected = card.isFavorite
        button.setImageResource(
            if (card.isFavorite) R.drawable.ic_home_heart_filled
            else R.drawable.ic_home_heart_outline
        )
        button.contentDescription = button.context.getString(
            if (card.isFavorite) R.string.home_remove_favorite else R.string.home_add_favorite,
            card.content.title
        )
        button.setOnClickListener { onFavorite(card.content, !card.isFavorite) }
    }

    private inner class ArtworkHolder(private val binding: ItemDemoWallpaperBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(card: DemoContentCard) {
            binding.artwork.setImageDrawable(artwork.drawable(requireNotNull(card.content.artworkIndex)))
            val params = binding.artwork.layoutParams as ConstraintLayout.LayoutParams
            params.dimensionRatio = if (card.content.type == ContentType.PROFILE_PICTURE) "H,1:1" else "H,9:16"
            binding.artwork.layoutParams = params
            binding.artwork.contentDescription = card.content.title
            binding.artwork.setOnClickListener { onPreview(card.content) }
            binding.playBadge.isVisible = card.content.type == ContentType.VIDEO_WALLPAPER
            bindFavorite(binding.favoriteButton, card)
        }
    }

    private inner class CompactArtworkHolder(
        private val binding: ItemSearchWallpaperBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(card: DemoContentCard) {
            binding.artwork.setImageDrawable(artwork.drawable(requireNotNull(card.content.artworkIndex)))
            val params = binding.artwork.layoutParams as ConstraintLayout.LayoutParams
            params.dimensionRatio = if (card.content.type == ContentType.PROFILE_PICTURE) "H,1:1" else "H,9:16"
            binding.artwork.layoutParams = params
            binding.artwork.contentDescription = card.content.title
            binding.artwork.setOnClickListener { onPreview(card.content) }
            binding.playBadge.isVisible = card.content.type == ContentType.VIDEO_WALLPAPER
            bindFavorite(binding.favoriteButton, card)
        }
    }

    private inner class TextHolder(private val binding: ItemDemoTextContentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(card: DemoContentCard) {
            val ringtone = card.content.type == ContentType.RINGTONE
            binding.contentIcon.setImageResource(if (ringtone) R.drawable.ic_nav_ringtone_selected else R.drawable.ic_nav_quotes_selected)
            binding.contentTitle.text = card.content.quote ?: card.content.title
            binding.contentSubtitle.text = if (ringtone) binding.root.context.getString(R.string.home_sound_duration) else card.content.title
            binding.root.setOnClickListener { onPreview(card.content) }
            bindFavorite(binding.favoriteButton, card)
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<DemoContentCard>() {
        override fun areItemsTheSame(oldItem: DemoContentCard, newItem: DemoContentCard) = oldItem.content.ref == newItem.content.ref
        override fun areContentsTheSame(oldItem: DemoContentCard, newItem: DemoContentCard) = oldItem == newItem
    }

    private companion object {
        const val VIEW_TYPE_ARTWORK = 0
        const val VIEW_TYPE_TEXT = 1
        const val VIEW_TYPE_COMPACT_ARTWORK = 2
    }
}

package com.cute.wallpaper.ringtones.presentation.home

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cute.wallpaper.ringtones.R
import com.cute.wallpaper.ringtones.domain.model.ContentType
import com.cute.wallpaper.ringtones.databinding.ItemRingtoneBinding
import com.cute.wallpaper.ringtones.databinding.ItemSearchWallpaperBinding
import com.cute.wallpaper.ringtones.databinding.ItemTextContentBinding
import com.cute.wallpaper.ringtones.databinding.ItemWallpaperBinding

data class ContentCard(
    val content: HomeContentUiModel,
    val isFavorite: Boolean,
    val isPlaying: Boolean = false,
    val isPreparing: Boolean = false,
    val playbackProgress: Int = 0
)

class ContentAdapter(
    private val onFavorite: (HomeContentUiModel, Boolean) -> Unit,
    private val onPreview: (HomeContentUiModel) -> Unit,
    private val compactArtwork: Boolean = false,
    private val onRingtoneSet: (HomeContentUiModel) -> Unit = {}
) : ListAdapter<ContentCard, RecyclerView.ViewHolder>(DiffCallback) {

    override fun getItemViewType(position: Int): Int = when {
        getItem(position).content.type == ContentType.RINGTONE -> VIEW_TYPE_RINGTONE
        getItem(position).content.thumbnailUrl == null -> VIEW_TYPE_TEXT
        compactArtwork -> VIEW_TYPE_COMPACT_ARTWORK
        else -> VIEW_TYPE_ARTWORK
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_ARTWORK -> ArtworkHolder(
                ItemWallpaperBinding.inflate(inflater, parent, false)
            )

            VIEW_TYPE_COMPACT_ARTWORK -> CompactArtworkHolder(
                ItemSearchWallpaperBinding.inflate(inflater, parent, false)
            )

            VIEW_TYPE_RINGTONE -> RingtoneHolder(
                ItemRingtoneBinding.inflate(inflater, parent, false)
            )

            else -> TextHolder(
                ItemTextContentBinding.inflate(inflater, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val card = getItem(position)
        when (holder) {
            is ArtworkHolder -> holder.bind(card)
            is CompactArtworkHolder -> holder.bind(card)
            is RingtoneHolder -> holder.bind(card)
            is TextHolder -> holder.bind(card)
        }
    }

    private fun bindFavorite(button: ImageView, card: ContentCard) {
        button.isSelected = card.isFavorite
        button.setImageResource(
            if (card.isFavorite) R.drawable.ic_heart_filled
            else R.drawable.ic_heart_outline
        )
        button.setOnClickListener {
            onFavorite(card.content, !card.isFavorite)
        }
    }

    private fun bindArtwork(imageView: ImageView, card: ContentCard) {
        val item = card.content
        val params = imageView.layoutParams as ConstraintLayout.LayoutParams
        params.dimensionRatio = if (item.type == ContentType.PROFILE_PICTURE) "H,1:1" else "H,9:16"
        imageView.layoutParams = params
        imageView.setOnClickListener { onPreview(item) }

        Glide.with(imageView)
            .load(item.thumbnailUrl ?: item.contentUrl)
            .centerCrop()
            .into(imageView)
    }

    private inner class ArtworkHolder(
        private val binding: ItemWallpaperBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(card: ContentCard) {
            bindArtwork(binding.artwork, card)
            binding.playBadge.isVisible =
                card.content.type == ContentType.VIDEO_WALLPAPER
            bindFavorite(binding.favoriteButton, card)
        }
    }

    private inner class CompactArtworkHolder(
        private val binding: ItemSearchWallpaperBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(card: ContentCard) {
            bindArtwork(binding.artwork, card)
            binding.playBadge.isVisible =
                card.content.type == ContentType.VIDEO_WALLPAPER
            bindFavorite(binding.favoriteButton, card)
        }
    }

    private inner class RingtoneHolder(
        private val binding: ItemRingtoneBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(card: ContentCard) {
            val item = card.content
            val backgroundIndex = Math.floorMod(item.id.hashCode(), ringtoneBackgrounds.size)
            binding.root.setBackgroundResource(ringtoneBackgrounds[backgroundIndex])
            binding.contentTitle.text = item.title
            binding.playLoading.isVisible = card.isPreparing
            binding.playButton.isVisible = !card.isPreparing
            binding.playContainer.setBackgroundResource(
                if (card.isPlaying) R.drawable.bg_ringtone_play_active
                else R.drawable.bg_home_action
            )
            binding.playButton.setImageResource(
                if (card.isPlaying) R.drawable.ic_ringtone_pause
                else R.drawable.ic_ringtone_play
            )
            binding.playButton.setOnClickListener { onPreview(item) }
            binding.root.setOnClickListener { onPreview(item) }
            binding.setButton.setOnClickListener { onRingtoneSet(item) }
            bindFavorite(binding.favoriteButton, card)
        }
    }

    private inner class TextHolder(
        private val binding: ItemTextContentBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(card: ContentCard) {
            val item = card.content
            val ringtone = item.type == ContentType.RINGTONE
            binding.contentIcon.setImageResource(
                if (ringtone) R.drawable.ic_nav_ringtone_selected
                else R.drawable.ic_nav_quotes_selected
            )
            binding.contentTitle.text = item.quote ?: item.title
            binding.contentSubtitle.text = if (ringtone) {
                item.category.replaceFirstChar(Char::titlecase)
            } else {
                item.title
            }
            binding.root.setOnClickListener { onPreview(item) }
            bindFavorite(binding.favoriteButton, card)
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ContentCard>() {
        override fun areItemsTheSame(
            oldItem: ContentCard,
            newItem: ContentCard
        ): Boolean = oldItem.content.ref == newItem.content.ref

        override fun areContentsTheSame(
            oldItem: ContentCard,
            newItem: ContentCard
        ): Boolean = oldItem == newItem
    }

    private companion object {
        const val VIEW_TYPE_ARTWORK = 0
        const val VIEW_TYPE_TEXT = 1
        const val VIEW_TYPE_COMPACT_ARTWORK = 2
        const val VIEW_TYPE_RINGTONE = 3
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

package com.cute.wallpaper.ringtones.presentation.favorites

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
import com.cute.wallpaper.ringtones.databinding.ItemFavoriteArtworkBinding
import com.cute.wallpaper.ringtones.databinding.ItemFavoriteRingtoneBinding
import com.cute.wallpaper.ringtones.domain.model.ContentType
import com.cute.wallpaper.ringtones.presentation.home.ContentCard
import com.cute.wallpaper.ringtones.presentation.home.HomeContentUiModel

class FavoritesContentAdapter(
    private val onFavorite: (HomeContentUiModel, Boolean) -> Unit,
    private val onOpen: (HomeContentUiModel) -> Unit,
    private val onSetRingtone: (HomeContentUiModel) -> Unit
) : ListAdapter<ContentCard, RecyclerView.ViewHolder>(Diff) {
    override fun getItemViewType(position: Int) = if (getItem(position).content.type == ContentType.RINGTONE) 1 else 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == 1) RingtoneHolder(ItemFavoriteRingtoneBinding.inflate(inflater, parent, false))
        else ArtworkHolder(ItemFavoriteArtworkBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ArtworkHolder -> holder.bind(getItem(position))
            is RingtoneHolder -> holder.bind(getItem(position))
        }
    }

    private fun bindFavorite(view: ImageView, card: ContentCard) {
        view.isSelected = card.isFavorite
        view.setImageResource(if (card.isFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline)
        view.setOnClickListener { onFavorite(card.content, !card.isFavorite) }
    }

    private inner class ArtworkHolder(private val binding: ItemFavoriteArtworkBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(card: ContentCard) {
            val params = binding.artwork.layoutParams as ConstraintLayout.LayoutParams
            params.dimensionRatio = if (card.content.type == ContentType.PROFILE_PICTURE) "H,1:1" else "H,29:54"
            binding.artwork.layoutParams = params
            Glide.with(binding.artwork).load(card.content.thumbnailUrl ?: card.content.contentUrl).centerCrop().into(binding.artwork)
            binding.artwork.setOnClickListener { onOpen(card.content) }
            binding.playBadge.isVisible = card.content.type == ContentType.VIDEO_WALLPAPER
            bindFavorite(binding.favoriteButton, card)
        }
    }

    private inner class RingtoneHolder(private val binding: ItemFavoriteRingtoneBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(card: ContentCard) {
            val background = ringtoneBackgrounds[
                bindingAdapterPosition.coerceAtLeast(0) % ringtoneBackgrounds.size
            ]
            binding.root.setBackgroundResource(background)
            binding.contentTitle.text = card.content.title
            binding.playLoading.isVisible = card.isPreparing
            binding.playButton.isVisible = !card.isPreparing
            binding.playButton.setImageResource(if (card.isPlaying) R.drawable.ic_ringtone_pause else R.drawable.ic_play)
            binding.playProgress.setProgressCompat(card.playbackProgress, false)
            binding.playButton.setOnClickListener { onOpen(card.content) }
            binding.root.setOnClickListener { onOpen(card.content) }
            binding.setButton.setOnClickListener { onSetRingtone(card.content) }
            bindFavorite(binding.favoriteButton, card)
        }
    }

    private val ringtoneBackgrounds = intArrayOf(
        R.drawable.bg_favorites_ringtone_pink,
        R.drawable.bg_favorites_ringtone_blue,
        R.drawable.bg_favorites_ringtone_orange,
        R.drawable.bg_favorites_ringtone_purple
    )

    private object Diff : DiffUtil.ItemCallback<ContentCard>() {
        override fun areItemsTheSame(oldItem: ContentCard, newItem: ContentCard) = oldItem.content.ref == newItem.content.ref
        override fun areContentsTheSame(oldItem: ContentCard, newItem: ContentCard) = oldItem == newItem
    }
}

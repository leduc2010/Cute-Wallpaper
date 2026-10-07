package com.cute.wallpaper.ringtones.presentation.profile

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bumptech.glide.Glide
import com.cute.wallpaper.ringtones.R
import com.cute.wallpaper.ringtones.databinding.ItemProfileAvatarBinding
import com.cute.wallpaper.ringtones.databinding.ItemProfileAvatarCaptionBinding
import com.cute.wallpaper.ringtones.databinding.ItemProfileBannerBinding
import com.cute.wallpaper.ringtones.databinding.ItemProfileEmptyBinding
import com.cute.wallpaper.ringtones.databinding.ItemProfileFeaturedBinding
import com.cute.wallpaper.ringtones.databinding.ItemProfileFiltersBinding
import com.cute.wallpaper.ringtones.presentation.home.HomeContentUiModel
import com.cute.wallpaper.ringtones.presentation.home.toDisplayColorHex
import com.google.android.material.chip.Chip

sealed interface ProfileFeedRow {
    val key: String
    data class Featured(val content: HomeContentUiModel, val favorite: Boolean) : ProfileFeedRow {
        override val key = "featured"
    }
    data object Banner : ProfileFeedRow { override val key = "banner" }
    data class Avatar(val content: HomeContentUiModel) : ProfileFeedRow {
        override val key = "avatar:${content.id}"
    }

    data class AvatarCaption(val content: HomeContentUiModel) : ProfileFeedRow {
        override val key = "avatar-caption:${content.id}"
    }
    data class Filters(val tags: List<String>, val selectedColor: String, val selectedTags: Set<String>) : ProfileFeedRow {
        override val key = "filters"
    }
    data class Empty(val searching: Boolean) : ProfileFeedRow { override val key = "empty" }
}

class ProfilePicturesAdapter(
    private val onOpen: (HomeContentUiModel) -> Unit,
    private val onFavorite: (HomeContentUiModel, Boolean) -> Unit,
    private val onSurprise: () -> Unit,
    private val onColor: (String?) -> Unit,
    private val onTag: (String, Boolean) -> Unit,
    private val onCloseFilters: () -> Unit
) : ListAdapter<ProfileFeedRow, RecyclerView.ViewHolder>(Diff) {
    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is ProfileFeedRow.Featured -> FEATURED
        ProfileFeedRow.Banner -> BANNER
        is ProfileFeedRow.Avatar -> AVATAR
        is ProfileFeedRow.AvatarCaption -> AVATAR_CAPTION
        is ProfileFeedRow.Filters -> FILTERS
        is ProfileFeedRow.Empty -> EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            FEATURED -> FeaturedHolder(ItemProfileFeaturedBinding.inflate(inflater, parent, false))
            BANNER -> BannerHolder(ItemProfileBannerBinding.inflate(inflater, parent, false))
            AVATAR -> AvatarHolder(ItemProfileAvatarBinding.inflate(inflater, parent, false))
            AVATAR_CAPTION -> AvatarCaptionHolder(
                ItemProfileAvatarCaptionBinding.inflate(inflater, parent, false)
            )
            FILTERS -> FiltersHolder(ItemProfileFiltersBinding.inflate(inflater, parent, false))
            else -> EmptyHolder(ItemProfileEmptyBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val row = getItem(position)
        (holder.itemView.layoutParams as? StaggeredGridLayoutManager.LayoutParams)?.isFullSpan =
            row !is ProfileFeedRow.Avatar && row !is ProfileFeedRow.AvatarCaption
        when (holder) {
            is FeaturedHolder -> holder.bind(row as ProfileFeedRow.Featured)
            is BannerHolder -> holder.bind()
            is AvatarHolder -> holder.bind(row as ProfileFeedRow.Avatar)
            is AvatarCaptionHolder -> holder.bind(row as ProfileFeedRow.AvatarCaption)
            is FiltersHolder -> holder.bind(row as ProfileFeedRow.Filters)
            is EmptyHolder -> holder.binding.message.setText(
                if ((row as ProfileFeedRow.Empty).searching) R.string.profile_search_no_results else R.string.profile_feed_empty
            )
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        when (holder) {
            is FeaturedHolder -> Glide.with(holder.binding.artwork).clear(holder.binding.artwork)
            is AvatarHolder -> Glide.with(holder.binding.artwork).clear(holder.binding.artwork)
            is AvatarCaptionHolder -> Glide.with(holder.binding.artwork).clear(holder.binding.artwork)
        }
        super.onViewRecycled(holder)
    }

    private inner class FeaturedHolder(val binding: ItemProfileFeaturedBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(row: ProfileFeedRow.Featured) {
            val item = row.content
            binding.title.text = item.title
            binding.tags.text = item.tags.joinToString(" · ")
            binding.tags.isVisible = item.tags.isNotEmpty()
            Glide.with(binding.artwork).load(item.thumbnailUrl ?: item.contentUrl).into(binding.artwork)
            binding.root.setOnClickListener { onOpen(item) }
            binding.useButton.setOnClickListener { onOpen(item) }
            binding.favoriteButton.isSelected = row.favorite
            binding.favoriteButton.setOnClickListener { onFavorite(item, !row.favorite) }
        }
    }

    private inner class BannerHolder(val binding: ItemProfileBannerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            binding.surpriseButton.setOnClickListener { onSurprise() }
        }
    }

    private inner class AvatarHolder(
        val binding: ItemProfileAvatarBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(row: ProfileFeedRow.Avatar) {
            val item = row.content
            val context = binding.root.context
            val neutral = ContextCompat.getColor(context, R.color.tertiary_50)
            val fill = if (item.color.isNullOrBlank()) {
                neutral
            } else {
                val source = Color.parseColor(item.color.toDisplayColorHex())
                ColorUtils.blendARGB(source, neutral, 0.82f)
            }

            binding.card.setCardBackgroundColor(fill)
            Glide.with(binding.artwork)
                .load(item.thumbnailUrl ?: item.contentUrl)
                .into(binding.artwork)
            binding.root.contentDescription = item.title
            binding.root.setOnClickListener { onOpen(item) }
        }
    }

    private inner class AvatarCaptionHolder(
        val binding: ItemProfileAvatarCaptionBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(row: ProfileFeedRow.AvatarCaption) {
            val item = row.content

            Glide.with(binding.artwork)
                .load(item.thumbnailUrl ?: item.contentUrl)
                .into(binding.artwork)
            binding.title.text = item.title
            binding.tags.text = item.tags.joinToString(" · ")
            binding.tags.isVisible = item.tags.isNotEmpty()
            binding.root.contentDescription = item.title
            binding.root.setOnClickListener { onOpen(item) }
        }
    }

    private inner class FiltersHolder(val binding: ItemProfileFiltersBinding) : RecyclerView.ViewHolder(binding.root) {
        private var updatingSelection = false
        fun bind(row: ProfileFeedRow.Filters) {
            updatingSelection = true
            val inflater = LayoutInflater.from(binding.root.context)
            binding.colorGroup.removeAllViews()
            COLORS.forEach { (id, label) ->
                val chip = inflater.inflate(R.layout.item_search_color_chip, binding.colorGroup, false) as Chip
                chip.id = View.generateViewId()
                chip.contentDescription = chip.context.getString(R.string.home_filter_color_description, chip.context.getString(label))
                val hex = if (id == "pink") "#FF6493" else id.toDisplayColorHex()
                chip.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor(hex))
                chip.chipStrokeColor = ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                    intArrayOf(ContextCompat.getColor(chip.context, R.color.primary_500), if (id == "white") Color.LTGRAY else Color.TRANSPARENT)
                )
                chip.isChecked = row.selectedColor == id
                chip.setOnCheckedChangeListener { _, checked ->
                    if (!updatingSelection) {
                        if (checked) onColor(id) else if (row.selectedColor == id) onColor(null)
                    }
                }
                binding.colorGroup.addView(chip)
            }
            binding.tagGroup.removeAllViews()
            row.tags.forEach { tag ->
                val chip = inflater.inflate(R.layout.item_search_genre_chip, binding.tagGroup, false) as Chip
                chip.id = View.generateViewId()
                chip.text = tag
                chip.setChipIconResource(R.drawable.ic_sparkles)
                chip.isChecked = tag in row.selectedTags
                chip.setOnCheckedChangeListener { _, checked -> if (!updatingSelection) onTag(tag, checked) }
                binding.tagGroup.addView(chip)
            }
            binding.closeButton.setOnClickListener { onCloseFilters() }
            updatingSelection = false
        }
    }

    private class EmptyHolder(val binding: ItemProfileEmptyBinding) : RecyclerView.ViewHolder(binding.root)

    private object Diff : DiffUtil.ItemCallback<ProfileFeedRow>() {
        override fun areItemsTheSame(oldItem: ProfileFeedRow, newItem: ProfileFeedRow) = oldItem.key == newItem.key
        override fun areContentsTheSame(oldItem: ProfileFeedRow, newItem: ProfileFeedRow) = oldItem == newItem
    }

    private companion object {
        const val FEATURED = 0
        const val BANNER = 1
        const val AVATAR = 2
        const val AVATAR_CAPTION = 3
        const val FILTERS = 4
        const val EMPTY = 5
        val COLORS = listOf(
            "pink" to R.string.profile_color_pink, "green" to R.string.profile_color_green,
            "blue" to R.string.profile_color_blue, "black" to R.string.profile_color_black,
            "purple" to R.string.profile_color_purple, "white" to R.string.profile_color_white,
            "red" to R.string.profile_color_red, "orange" to R.string.profile_color_orange,
            "cyan" to R.string.profile_color_cyan, "yellow" to R.string.profile_color_yellow
        )
    }
}

package com.cute.wallpaper.ringtones.presentation.quotes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cute.wallpaper.ringtones.R
import com.cute.wallpaper.ringtones.databinding.ItemQuoteCategoryBinding
import com.cute.wallpaper.ringtones.databinding.ItemQuoteEmptyBinding
import com.cute.wallpaper.ringtones.databinding.ItemQuoteFeaturedCategoryBinding
import com.cute.wallpaper.ringtones.presentation.home.HomeContentUiModel

internal sealed interface QuoteFeedRow {
    val key: String
    data class Featured(val card: QuoteCard) : QuoteFeedRow { override val key = "featured" }
    data class Card(val card: QuoteCard) : QuoteFeedRow { override val key = "quote:${card.content.id}" }
    data class FeaturedCategory(val category: QuoteCategoryUiModel) : QuoteFeedRow { override val key = "featured-category" }
    data class Category(val category: QuoteCategoryUiModel) : QuoteFeedRow { override val key = "category:${category.id}" }
    data class Empty(val searching: Boolean) : QuoteFeedRow { override val key = "empty" }
}

internal class QuotesFeedAdapter(
    private val onFavorite: (HomeContentUiModel, Boolean) -> Unit,
    private val onOpen: (HomeContentUiModel) -> Unit,
    private val onCopy: (HomeContentUiModel) -> Unit,
    private val onShare: (HomeContentUiModel) -> Unit,
    private val onCategory: (QuoteCategoryUiModel) -> Unit
) : ListAdapter<QuoteFeedRow, RecyclerView.ViewHolder>(Diff) {
    fun isFullSpan(position: Int) = getItem(position) !is QuoteFeedRow.Card && getItem(position) !is QuoteFeedRow.Category

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is QuoteFeedRow.Featured -> 0
        is QuoteFeedRow.Card -> 1
        is QuoteFeedRow.FeaturedCategory -> 2
        is QuoteFeedRow.Category -> 3
        is QuoteFeedRow.Empty -> 4
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0, 1 -> QuoteViewHolder.create(parent, onFavorite, onOpen, onCopy, onShare)
            2 -> FeaturedCategoryHolder(ItemQuoteFeaturedCategoryBinding.inflate(inflater, parent, false))
            3 -> CategoryHolder(ItemQuoteCategoryBinding.inflate(inflater, parent, false))
            else -> EmptyHolder(ItemQuoteEmptyBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is QuoteFeedRow.Featured -> (holder as QuoteViewHolder).bind(row.card, featured = true)
            is QuoteFeedRow.Card -> (holder as QuoteViewHolder).bind(row.card)
            is QuoteFeedRow.FeaturedCategory -> (holder as FeaturedCategoryHolder).bind(row.category)
            is QuoteFeedRow.Category -> (holder as CategoryHolder).bind(row.category)
            is QuoteFeedRow.Empty -> (holder as EmptyHolder).binding.message.setText(
                if (row.searching) R.string.quotes_search_empty else R.string.quotes_empty)
        }
    }

    private inner class FeaturedCategoryHolder(val binding: ItemQuoteFeaturedCategoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(category: QuoteCategoryUiModel) {
            val label = category.name.replaceFirstChar(Char::titlecase)
            binding.title.text = label
            binding.explore.text = binding.root.context.getString(R.string.quotes_explore_category, label)
            bindThumbnail(binding.thumbnail, binding.thumbnailOverlay, category.thumbnailUrl)
            binding.explore.setOnClickListener { onCategory(category) }
            binding.root.setOnClickListener { onCategory(category) }
        }
    }
    private inner class CategoryHolder(val binding: ItemQuoteCategoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(category: QuoteCategoryUiModel) {
            binding.title.text = category.name.replaceFirstChar(Char::titlecase)
            bindThumbnail(binding.thumbnail, binding.thumbnailOverlay, category.thumbnailUrl)
            binding.root.setOnClickListener { onCategory(category) }
        }
    }

    private fun bindThumbnail(image: android.widget.ImageView, overlay: android.view.View, url: String?) {
        val hasThumbnail = !url.isNullOrBlank()
        image.isVisible = hasThumbnail
        overlay.isVisible = hasThumbnail
        if (hasThumbnail) {
            Glide.with(image).load(url).centerCrop().into(image)
        } else {
            Glide.with(image).clear(image)
        }
    }
    private class EmptyHolder(val binding: ItemQuoteEmptyBinding) : RecyclerView.ViewHolder(binding.root)
    private object Diff : DiffUtil.ItemCallback<QuoteFeedRow>() {
        override fun areItemsTheSame(oldItem: QuoteFeedRow, newItem: QuoteFeedRow) = oldItem.key == newItem.key
        override fun areContentsTheSame(oldItem: QuoteFeedRow, newItem: QuoteFeedRow) = oldItem == newItem
    }
}

package com.cute.wallpaper.ringtones.presentation.quotes

import android.view.LayoutInflater
import android.view.ViewGroup
import android.util.TypedValue
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cute.wallpaper.ringtones.R
import com.cute.wallpaper.ringtones.databinding.ItemQuoteCardBinding
import com.cute.wallpaper.ringtones.presentation.home.HomeContentUiModel

data class QuoteCard(val content: HomeContentUiModel, val favorite: Boolean)

class QuoteAdapter(
    private val onFavorite: (HomeContentUiModel, Boolean) -> Unit,
    private val onOpen: (HomeContentUiModel) -> Unit,
    private val onCopy: (HomeContentUiModel) -> Unit,
    private val onShare: (HomeContentUiModel) -> Unit,
    private val showAttribution: Boolean = false
) : ListAdapter<QuoteCard, QuoteViewHolder>(Diff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        QuoteViewHolder.create(parent, onFavorite, onOpen, onCopy, onShare)
    override fun onBindViewHolder(holder: QuoteViewHolder, position: Int) = holder.bind(getItem(position), showAttribution = showAttribution)
    private object Diff : DiffUtil.ItemCallback<QuoteCard>() {
        override fun areItemsTheSame(oldItem: QuoteCard, newItem: QuoteCard) = oldItem.content.ref == newItem.content.ref
        override fun areContentsTheSame(oldItem: QuoteCard, newItem: QuoteCard) = oldItem == newItem
    }
}

class QuoteViewHolder private constructor(
    private val binding: ItemQuoteCardBinding,
    private val onFavorite: (HomeContentUiModel, Boolean) -> Unit,
    private val onOpen: (HomeContentUiModel) -> Unit,
    private val onCopy: (HomeContentUiModel) -> Unit,
    private val onShare: (HomeContentUiModel) -> Unit
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(card: QuoteCard, featured: Boolean = false, showAttribution: Boolean = false) {
        val context = binding.root.context
        val density = context.resources.displayMetrics.density
        binding.root.layoutParams.height = ((if (featured) 160 else 260) * density).toInt()
        binding.root.setCardBackgroundColor(ContextCompat.getColor(context,
            if (featured) R.color.alpha_light_10 else android.R.color.transparent))
        binding.cardContent.updatePadding(left = ((if (featured) 20 else 16) * density).toInt(),
            right = ((if (featured) 20 else 16) * density).toInt(),
            bottom = ((if (featured) 20 else 16) * density).toInt())
        (binding.quoteMark.layoutParams as ConstraintLayout.LayoutParams).topMargin = ((if (featured) 20 else 28) * density).toInt()
        (binding.quoteText.layoutParams as ConstraintLayout.LayoutParams).topMargin = ((if (featured) 8 else 4) * density).toInt()
        binding.quoteText.layoutParams = (binding.quoteText.layoutParams as ConstraintLayout.LayoutParams).apply {
            height = ViewGroup.LayoutParams.WRAP_CONTENT
            bottomToTop = ConstraintLayout.LayoutParams.UNSET
        }
        binding.featuredBadge.translationX = if (featured) -8 * density else 0f
        binding.featuredBadge.isVisible = featured
        val star = AppCompatResources.getDrawable(context, R.drawable.ic_star)?.apply {
            setBounds(0, 0, (12 * density).toInt(), (12 * density).toInt())
        }
        binding.featuredBadge.setCompoundDrawables(star, null, null, null)
        binding.featuredBadge.compoundDrawablePadding = (4 * density).toInt()
        binding.attribution.isVisible = showAttribution
        binding.separator.isVisible = showAttribution
        binding.quoteMark.textSize = if (featured) 40f else 28f
        TextViewCompat.setTextAppearance(binding.quoteText, if (featured) R.style.Body16S else R.style.Body14S)
        val lineHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
            if (featured) 24f else 21f, context.resources.displayMetrics).toInt()
        TextViewCompat.setLineHeight(binding.quoteText, lineHeight)
        val bodyHeight = ((if (featured) 56 else if (showAttribution) 116 else 156) * density).toInt()
        binding.quoteText.maxHeight = bodyHeight
        binding.quoteText.maxLines = minOf(if (featured) 2 else if (showAttribution) 4 else 7,
            (bodyHeight / lineHeight.coerceAtLeast(1)).coerceAtLeast(1))
        binding.quoteText.text = card.content.quote.orEmpty()
        val hasThumbnail = !card.content.thumbnailUrl.isNullOrBlank()
        binding.thumbnail.isVisible = hasThumbnail
        binding.thumbnailOverlay.isVisible = hasThumbnail
        if (hasThumbnail) {
            Glide.with(binding.thumbnail)
                .load(card.content.thumbnailUrl)
                .centerCrop()
                .into(binding.thumbnail)
        } else {
            Glide.with(binding.thumbnail).clear(binding.thumbnail)
        }
        binding.root.contentDescription = card.content.quote
        binding.root.setOnClickListener { onOpen(card.content) }
        binding.btnFavorite.isSelected = card.favorite
        binding.btnFavorite.contentDescription = context.getString(
            if (card.favorite) R.string.home_remove_favorite else R.string.home_add_favorite, card.content.title)
        binding.btnFavorite.setOnClickListener { onFavorite(card.content, !card.favorite) }
        binding.btnPreview.setOnClickListener { onOpen(card.content) }
        binding.btnCopy.setOnClickListener { onCopy(card.content) }
        binding.btnShare.setOnClickListener { onShare(card.content) }
    }

    companion object {
        fun create(parent: ViewGroup,
            onFavorite: (HomeContentUiModel, Boolean) -> Unit,
            onOpen: (HomeContentUiModel) -> Unit,
            onCopy: (HomeContentUiModel) -> Unit,
            onShare: (HomeContentUiModel) -> Unit
        ) = QuoteViewHolder(ItemQuoteCardBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onFavorite, onOpen, onCopy, onShare)
    }
}

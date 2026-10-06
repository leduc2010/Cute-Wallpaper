package com.cute.wallpaper.ringtones.presentation.language

import android.graphics.Matrix
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.cute.wallpaper.ringtones.databinding.ItemLanguageBinding

class LanguageAdapter(
    private val items: List<LanguageItem>,
    selectedLanguageCode: String?,
    private val showHandClick: Boolean = false,
    private val onSelected: (LanguageItem) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {
    var selectedLanguageCode: String? = selectedLanguageCode
        ?.takeIf(String::isNotBlank)
        ?.let(LanguageOptions::canonicalCode)
        private set

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = LanguageViewHolder(
        ItemLanguageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) = holder.bind(items[position])
    override fun getItemCount(): Int = items.size
    override fun onViewAttachedToWindow(holder: LanguageViewHolder) = holder.updateHand()
    override fun onViewDetachedFromWindow(holder: LanguageViewHolder) = holder.stopHand()
    override fun onViewRecycled(holder: LanguageViewHolder) = holder.stopHand()

    private fun select(item: LanguageItem) {
        val oldPosition = items.indexOfFirst { it.code == selectedLanguageCode }
        val newPosition = items.indexOfFirst { it.code == item.code }
        if (newPosition < 0 || oldPosition == newPosition) return
        val handPosition = if (selectedLanguageCode == null) items.indexOfFirst { it.code == "en" } else -1
        selectedLanguageCode = item.code
        setOf(oldPosition, newPosition, handPosition).filter { it >= 0 }.forEach(::notifyItemChanged)
        onSelected(item)
    }

    inner class LanguageViewHolder(private val binding: ItemLanguageBinding) : RecyclerView.ViewHolder(binding.root) {
        private var language: LanguageItem? = null

        fun bind(item: LanguageItem) {
            language = item
            binding.languageName.text = item.displayName
            binding.languageFlag.setImageResource(item.flagRes)
            binding.radioLanguage.isSelected = item.code == selectedLanguageCode
            binding.root.isSelected = item.code == selectedLanguageCode
            binding.root.contentDescription = item.displayName
            binding.root.setOnClickListener { select(item) }
            updateHand()
        }

        fun stopHand() {
            binding.handView.cancelAnimation()
            binding.handView.isVisible = false
        }

        fun updateHand() {
            if (!showHandClick || selectedLanguageCode != null || language?.code != "en") {
                stopHand()
                return
            }
            binding.handView.visibility = android.view.View.INVISIBLE
            binding.handView.doOnLayout { hand ->
                if (!showHandClick || selectedLanguageCode != null || language?.code != "en" || !binding.root.isAttachedToWindow) return@doOnLayout
                val radio = binding.radioLanguage
                // Tap ring anchor from the LS125 hand_tap.json (500 x 500).
                val tap = floatArrayOf(hand.width * 246f / 500f, hand.height * 197f / 500f)
                Matrix().apply { setRotate(hand.rotation, hand.pivotX, hand.pivotY); mapPoints(tap) }
                hand.translationX = radio.left + radio.width / 2f - hand.left - tap[0]
                hand.translationY = radio.top + radio.height / 2f - hand.top - tap[1]
                binding.handView.isVisible = true
                if (!binding.handView.isAnimating) binding.handView.playAnimation()
            }
        }
    }
}

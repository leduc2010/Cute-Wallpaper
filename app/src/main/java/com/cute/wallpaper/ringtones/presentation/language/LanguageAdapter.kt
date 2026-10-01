package com.cute.wallpaper.ringtones.presentation.language

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cute.wallpaper.ringtones.databinding.ItemLanguageBinding

class LanguageAdapter(
    private val items: List<LanguageItem>,
    selectedLanguageCode: String?,
    private val onSelected: (LanguageItem) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    var selectedLanguageCode: String? = selectedLanguageCode
        private set

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        return LanguageViewHolder(
            ItemLanguageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    private fun select(item: LanguageItem) {
        val oldPosition = items.indexOfFirst { it.code == selectedLanguageCode }
        val newPosition = items.indexOfFirst { it.code == item.code }
        if (newPosition < 0 || oldPosition == newPosition) return

        selectedLanguageCode = item.code
        if (oldPosition >= 0) notifyItemChanged(oldPosition)
        notifyItemChanged(newPosition)
        onSelected(item)
    }

    inner class LanguageViewHolder(
        private val binding: ItemLanguageBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: LanguageItem) {
            binding.radioLanguage.text = item.displayName
            binding.radioLanguage.isChecked = item.code == selectedLanguageCode
            binding.root.setOnClickListener { select(item) }
            binding.radioLanguage.setOnClickListener { select(item) }
        }
    }
}

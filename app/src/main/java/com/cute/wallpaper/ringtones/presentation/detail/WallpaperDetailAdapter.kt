package com.cute.wallpaper.ringtones.presentation.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cute.wallpaper.ringtones.databinding.ItemWallpaperDetailBinding
import com.cute.wallpaper.ringtones.presentation.home.HomeContentUiModel

class WallpaperDetailAdapter(
    private val items: List<HomeContentUiModel>
) : RecyclerView.Adapter<WallpaperDetailAdapter.WallpaperHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WallpaperHolder {
        return WallpaperHolder(
            ItemWallpaperDetailBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: WallpaperHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class WallpaperHolder(
        private val binding: ItemWallpaperDetailBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HomeContentUiModel) {
            Glide.with(binding.wallpaperImage)
                .load(item.contentUrl ?: item.thumbnailUrl)
                .centerCrop()
                .into(binding.wallpaperImage)
        }
    }
}

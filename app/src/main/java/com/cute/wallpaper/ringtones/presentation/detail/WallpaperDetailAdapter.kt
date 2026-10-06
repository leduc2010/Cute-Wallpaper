package com.cute.wallpaper.ringtones.presentation.detail

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.core.view.isVisible
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.cute.wallpaper.ringtones.databinding.ItemWallpaperDetailBinding
import com.cute.wallpaper.ringtones.presentation.home.HomeContentUiModel

@OptIn(UnstableApi::class)
class WallpaperDetailAdapter(
    private val items: List<HomeContentUiModel>,
    private val requestManager: RequestManager,
    private val enableMediaPreview: Boolean = false,
    private val videoPlayerProvider: () -> ExoPlayer? = { null },
    private val onPreviewError: () -> Unit = {}
) : RecyclerView.Adapter<WallpaperDetailAdapter.WallpaperHolder>() {

    private var playingPosition = RecyclerView.NO_POSITION
    private var previewEnabled = enableMediaPreview
    private var activeVideoPlayer: ExoPlayer? = null
    private var activePlayerView: PlayerView? = null

    private val playerListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            handlePreviewError(playingPosition)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WallpaperHolder {
        return WallpaperHolder(
            binding = ItemWallpaperDetailBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            requestManager = requestManager
        )
    }

    override fun onBindViewHolder(holder: WallpaperHolder, position: Int) {
        holder.bind(items[position], position == playingPosition)
    }

    override fun getItemCount(): Int = items.size

    override fun onViewRecycled(holder: WallpaperHolder) {
        if (enableMediaPreview && holder.boundPosition == playingPosition) {
            releaseVideoPlayerTarget()
            playingPosition = RecyclerView.NO_POSITION
        }
        holder.recycle()
        super.onViewRecycled(holder)
    }

    fun stopPreview() {
        val previousPosition = playingPosition
        playingPosition = RecyclerView.NO_POSITION
        releaseVideoPlayerTarget()
        if (previousPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(previousPosition)
        }
    }

    fun setPreviewEnabled(enabled: Boolean) {
        val effectiveEnabled = enableMediaPreview && enabled
        if (previewEnabled == effectiveEnabled) return
        previewEnabled = effectiveEnabled
        if (!effectiveEnabled) {
            stopPreview()
        }
        notifyItemRangeChanged(0, itemCount)
    }

    inner class WallpaperHolder(
        private val binding: ItemWallpaperDetailBinding,
        private val requestManager: RequestManager
    ) : RecyclerView.ViewHolder(binding.root) {

        var boundPosition: Int = RecyclerView.NO_POSITION
            private set

        init {
            binding.btnPlay.setOnClickListener {
                if (!previewEnabled) return@setOnClickListener
                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION || position == playingPosition) {
                    return@setOnClickListener
                }

                val previousPosition = playingPosition
                playingPosition = position
                releaseVideoPlayerTarget()

                if (previousPosition != RecyclerView.NO_POSITION) {
                    notifyItemChanged(previousPosition)
                }
                startPreview(items[position])
            }
        }

        fun bind(item: HomeContentUiModel, isPlaying: Boolean) {
            boundPosition = bindingAdapterPosition
            releasePreview()
            binding.btnPlay.isEnabled = previewEnabled
            binding.btnPlay.isVisible = previewEnabled

            requestManager
                .asBitmap()
                .load(item.thumbnailUrl ?: item.contentUrl)
                .centerCrop()
                .into(binding.wallpaperImage)

            if (isPlaying && previewEnabled) {
                startPreview(item)
            }
        }

        fun recycle() {
            requestManager.clear(binding.wallpaperImage)
            if (enableMediaPreview) {
                releasePreview()
            }
        }

        fun releasePreview() {
            requestManager.clear(binding.gifPreview)
            binding.gifPreview.isVisible = false

            if (binding.videoPreview === activePlayerView) {
                releaseVideoPlayerTarget()
            } else {
                binding.videoPreview.player = null
            }
            binding.videoPreview.isVisible = false

            binding.btnPlay.isVisible = previewEnabled
        }

        private fun startPreview(item: HomeContentUiModel) {
            val url = item.contentUrl?.takeIf(String::isNotBlank)
            if (url == null) {
                handlePreviewError(bindingAdapterPosition)
                return
            }

            if (url.isGifUrl()) {
                startGifPreview(url)
            } else {
                startVideoPreview(url)
            }
        }

        private fun startGifPreview(url: String) {
            binding.gifPreview.isVisible = true
            requestManager
                .asGif()
                .load(url)
                .centerCrop()
                .listener(object : RequestListener<GifDrawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<GifDrawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        handlePreviewError(bindingAdapterPosition)
                        return false
                    }

                    override fun onResourceReady(
                        resource: GifDrawable,
                        model: Any,
                        target: Target<GifDrawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding.btnPlay.isVisible = false
                        return false
                    }
                })
                .into(binding.gifPreview)
        }

        private fun startVideoPreview(url: String) {
            val player = videoPlayerProvider()
            if (player == null) {
                handlePreviewError(bindingAdapterPosition)
                return
            }

            if (activeVideoPlayer !== player) {
                activeVideoPlayer?.removeListener(playerListener)
                activeVideoPlayer = player
                player.addListener(playerListener)
            }

            binding.videoPreview.isVisible = true
            PlayerView.switchTargetView(player, activePlayerView, binding.videoPreview)
            activePlayerView = binding.videoPreview

            player.repeatMode = Player.REPEAT_MODE_ONE
            player.setMediaItem(MediaItem.fromUri(url))
            player.prepare()
            player.play()

            binding.btnPlay.isVisible = false
        }
    }

    private fun releaseVideoPlayerTarget() {
        activePlayerView?.player = null
        activePlayerView = null

        activeVideoPlayer?.apply {
            removeListener(playerListener)
            stop()
            clearMediaItems()
        }
        activeVideoPlayer = null
    }

    private fun handlePreviewError(position: Int) {
        val failedPosition = position.takeIf { it != RecyclerView.NO_POSITION } ?: playingPosition
        playingPosition = RecyclerView.NO_POSITION
        releaseVideoPlayerTarget()

        if (failedPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(failedPosition)
        }
        onPreviewError()
    }

    private fun String.isGifUrl(): Boolean =
        Uri.parse(this).path?.lowercase()?.endsWith(".gif") == true
}

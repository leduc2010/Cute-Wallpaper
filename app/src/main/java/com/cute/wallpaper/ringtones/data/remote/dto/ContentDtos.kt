package com.cute.wallpaper.ringtones.data.remote.dto

import com.google.gson.annotations.SerializedName

data class WallpaperDto(
    @SerializedName("id")
    val id: Int = 0,
    @SerializedName("category")
    val category: String = "",
    @SerializedName("thumb")
    val thumb: String = "",
    @SerializedName("content")
    val content: String = "",
    @SerializedName("content_2")
    val content2: String? = null,
    @SerializedName("rank")
    val rank: Int = 0,
    @SerializedName("tag")
    val tag: WallpaperTagDto? = null,
    @SerializedName("color")
    val color: String? = null,
    @SerializedName("download")
    val download: Boolean = false
)

data class WallpaperTagDto(
    @SerializedName("tags")
    val tags: List<String> = emptyList()
)

data class SoundDto(
    @SerializedName("id")
    val id: Int = 0,
    @SerializedName("category")
    val category: String = "",
    @SerializedName("name")
    val name: String = "",
    @SerializedName("link")
    val link: String = "",
    @SerializedName("rank")
    val rank: Int = 0,
    @SerializedName("tag")
    val tag: String? = null
)

data class QuoteDto(
    @SerializedName("id")
    val id: Int = 0,
    @SerializedName("category")
    val category: String = "",
    @SerializedName("content")
    val content: String = "",
    @SerializedName("rank")
    val rank: Int = 0,
    @SerializedName("tag")
    val tag: String? = null
)

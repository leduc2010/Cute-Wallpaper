package com.cute.wallpaper.ringtones.presentation.search

enum class SearchPageKind { COLOR, GENRE, CATEGORY }

data class SearchPageUiModel(
    val id: String,
    val label: String,
    val emoji: String,
    val kind: SearchPageKind,
    val colorHex: String = ""
)

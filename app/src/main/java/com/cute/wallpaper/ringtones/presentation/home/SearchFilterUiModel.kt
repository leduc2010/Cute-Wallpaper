package com.cute.wallpaper.ringtones.presentation.home

data class SearchFilterUiModel(
    val colors: List<SearchColorUiModel>,
    val genres: List<SearchGenreUiModel>,
    val quickFilters: List<SearchQuickFilterUiModel>
)

data class SearchColorUiModel(
    val id: String,
    val label: String,
    val colorHex: String
)

data class SearchGenreUiModel(
    val id: String,
    val label: String,
    val emoji: String
)

enum class SearchQuickFilterKind { COLOR, GENRE }

data class SearchQuickFilterUiModel(
    val id: String,
    val label: String,
    val emoji: String,
    val kind: SearchQuickFilterKind
)

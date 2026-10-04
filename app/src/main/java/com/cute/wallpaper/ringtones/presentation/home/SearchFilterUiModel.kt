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

fun String.toDisplayColorHex(): String = when (lowercase()) {
    "black" -> "#000000"
    "white" -> "#FFFFFF"
    "red" -> "#F44336"
    "green" -> "#4CAF50"
    "blue" -> "#2196F3"
    "pink" -> "#E91E63"
    "orange" -> "#FF9800"
    "teal" -> "#009688"
    "purple" -> "#9C27B0"
    "yellow" -> "#FFEB3B"
    "cyan" -> "#00BCD4"
    else -> "#EC5B8C"
}

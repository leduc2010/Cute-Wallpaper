package com.cute.wallpaper.ringtones.domain.model

data class ContentRef(
    val type: ContentType,
    val id: String
) {
    init {
        require(id.isNotBlank()) { "Content id must not be blank" }
    }

    fun toFavoriteKey(): String = "${type.name}:$id"

    companion object {
        fun fromFavoriteKey(value: String): ContentRef? {
            val separatorIndex = value.indexOf(':')
            if (separatorIndex <= 0 || separatorIndex == value.lastIndex) return null

            val type = runCatching {
                ContentType.valueOf(value.substring(0, separatorIndex))
            }.getOrNull() ?: return null

            val id = value.substring(separatorIndex + 1)
            if (id.isBlank()) return null

            return ContentRef(type = type, id = id)
        }
    }
}

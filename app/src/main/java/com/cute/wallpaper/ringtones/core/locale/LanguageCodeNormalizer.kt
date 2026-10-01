package com.cute.wallpaper.ringtones.core.locale

import java.util.Locale

object LanguageCodeNormalizer {
    fun normalize(code: String): String {
        val normalized = code.trim().replace('_', '-')
        if (normalized.isBlank()) return "en"

        val lower = normalized.lowercase(Locale.ROOT)
        val mapped = when {
            lower == "in" -> "id"
            lower.startsWith("in-") -> "id-${lower.substringAfter('-')}"
            lower == "fil" || lower.startsWith("fil-") ||
                lower == "tl" || lower.startsWith("tl-") -> "en-ph"
            else -> lower
        }

        val parts = mapped.split('-').filter { it.isNotBlank() }
        if (parts.isEmpty()) return "en"
        return buildString {
            append(parts.first().lowercase(Locale.ROOT))
            parts.drop(1).forEach { part ->
                append('-')
                append(if (part.length == 2) part.uppercase(Locale.ROOT) else part)
            }
        }
    }
}

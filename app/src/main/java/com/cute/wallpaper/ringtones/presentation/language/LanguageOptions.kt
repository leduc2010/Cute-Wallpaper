package com.cute.wallpaper.ringtones.presentation.language

import com.cute.wallpaper.ringtones.R
import java.util.Locale

/** LS125 language picker contract, shared by first launch and Settings. */
object LanguageOptions {
    val items = listOf(
        LanguageItem("fr", "Français", R.drawable.ic_flag_france),
        LanguageItem("en", "English", R.drawable.ic_flag_english),
        LanguageItem("hi", "मराठी (India)", R.drawable.ic_flag_india),
        LanguageItem("es", "Espanol", R.drawable.ic_flag_espanol),
        LanguageItem("zh", "Chinese", R.drawable.ic_flag_china),
        LanguageItem("pt-PT", "Português (Portugal)", R.drawable.ic_flag_portugal),
        LanguageItem("ru", "Русский", R.drawable.ic_flag_russia),
        LanguageItem("id", "Indonesian", R.drawable.ic_flag_indonesian),
        LanguageItem("en-PH", "Philippines", R.drawable.ic_flag_philippines),
        LanguageItem("bn", "বাংলা", R.drawable.ic_flag_bengal),
        LanguageItem("pt-BR", "Português (Brazil)", R.drawable.ic_flag_brazil),
        LanguageItem("af-ZA", "Afrikaans", R.drawable.ic_flag_afrikaans),
        LanguageItem("de", "Deutsch", R.drawable.ic_flag_deutsch),
        LanguageItem("en-CA", "Canada", R.drawable.ic_flag_canada),
        LanguageItem("en-GB", "English", R.drawable.ic_flag_uk),
        LanguageItem("ko", "Korean", R.drawable.ic_flag_korean),
        LanguageItem("nl", "Dutch", R.drawable.ic_flag_ducth),
        LanguageItem("vi", "Vietnamese", R.drawable.ic_flag_vi),
        LanguageItem("ar", "عربي", R.drawable.ic_flag_ar)
    )

    fun forDevice(languageTag: String): List<LanguageItem> {
        val locale = Locale.forLanguageTag(languageTag.trim().replace('_', '-'))
        val deviceTag = canonicalCode(locale.toLanguageTag())
        val deviceLanguage = canonicalCode(locale.language)
        val india = items.first { it.code == INDIA_CODE }
        val first = if (locale.language.equals(ENGLISH_CODE, ignoreCase = true)) {
            india
        } else {
            items.firstOrNull { canonicalCode(it.code) == deviceTag }
                ?: items.firstOrNull { canonicalCode(it.code).substringBefore('-') == deviceLanguage }
                ?: india
        }
        val english = items.first { it.code == ENGLISH_CODE }
        return listOf(first, english) + items.filterNot { it == first || it == english }
    }

    /** Aliases used by the LS125 picker; keep persisted Indonesian codes canonical. */
    fun canonicalCode(code: String): String {
        val normalized = code.trim().replace('_', '-').lowercase(Locale.ROOT)
        val mapped = when {
            normalized == "in" -> "id"
            normalized.startsWith("in-") -> "id-${normalized.substringAfter('-')}"
            normalized == "mr" || normalized.startsWith("mr-") -> INDIA_CODE
            normalized == "fil" || normalized.startsWith("fil-") ||
                normalized == "tl" || normalized.startsWith("tl-") -> "en-ph"
            else -> normalized
        }
        return Locale.forLanguageTag(mapped).toLanguageTag().takeUnless { it == "und" }.orEmpty()
    }

    fun displayName(code: String?): String {
        val normalized = canonicalCode(code.orEmpty())
        return items.firstOrNull { it.code == normalized }?.displayName
            ?: items.first { it.code == ENGLISH_CODE }.displayName
    }

    private const val ENGLISH_CODE = "en"
    private const val INDIA_CODE = "hi"
}

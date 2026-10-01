package com.cute.wallpaper.ringtones.core.locale

import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageCodeNormalizerTest {
    @Test
    fun normalize_canonicalizesTags() {
        assertEquals("en-US", LanguageCodeNormalizer.normalize("EN_us"))
        assertEquals("pt-BR", LanguageCodeNormalizer.normalize("pt-br"))
    }

    @Test
    fun normalize_mapsLegacyIndonesianCode() {
        assertEquals("id", LanguageCodeNormalizer.normalize("in"))
        assertEquals("id-ID", LanguageCodeNormalizer.normalize("in-ID"))
    }

    @Test
    fun normalize_mapsFilipinoAliases() {
        assertEquals("en-PH", LanguageCodeNormalizer.normalize("fil"))
        assertEquals("en-PH", LanguageCodeNormalizer.normalize("tl-PH"))
    }
}

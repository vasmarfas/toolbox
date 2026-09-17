package com.vasmarfas.card.tools.developer

import com.vasmarfas.card.resources.english
import com.vasmarfas.card.resources.matches
import com.vasmarfas.card.resources.russian
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RegexBuilderTest {
    @Test
    fun escapesLiteralText() {
        assertEquals("a\\.b", RegexBuilder.render(RegexBlock(RegexBlockKind.LITERAL, text = "a.b")))
        assertEquals("\\(1\\+1\\)", RegexBuilder.render(RegexBlock(RegexBlockKind.LITERAL, text = "(1+1)")))
        assertEquals("[a-f\\]]", RegexBuilder.render(RegexBlock(RegexBlockKind.CHARACTERS, set = RegexCharSet.CUSTOM, text = "a-f]")))
        assertEquals("", RegexBuilder.render(RegexBlock(RegexBlockKind.LITERAL)))
        assertEquals("", RegexBuilder.render(RegexBlock(RegexBlockKind.GROUP)))
    }

    @Test
    fun wrapsMultiCharLiteralsUnderAQuantifier() {
        assertEquals("a?", RegexBuilder.render(RegexBlock(RegexBlockKind.LITERAL, text = "a", quantifier = RegexQuantifier.OPTIONAL)))
        assertEquals("(?:ab)+", RegexBuilder.render(RegexBlock(RegexBlockKind.LITERAL, text = "ab", quantifier = RegexQuantifier.ONE_OR_MORE)))
        assertEquals("ab", RegexBuilder.render(RegexBlock(RegexBlockKind.LITERAL, text = "ab")))
    }

    @Test
    fun buildsQuantifiers() {
        val digit = RegexBlock(RegexBlockKind.CHARACTERS, set = RegexCharSet.DIGIT)
        assertEquals("\\d*", RegexBuilder.render(digit.copy(quantifier = RegexQuantifier.ZERO_OR_MORE)))
        assertEquals("\\d{4}", RegexBuilder.render(digit.copy(quantifier = RegexQuantifier.EXACTLY, min = "4")))
        assertEquals("\\d{2,5}", RegexBuilder.render(digit.copy(quantifier = RegexQuantifier.RANGE, min = "2", max = "5")))
        assertEquals("\\d{3,}", RegexBuilder.render(digit.copy(quantifier = RegexQuantifier.RANGE, min = "3", max = "")))
    }

    @Test
    fun composesBlocksIntoAPattern() {
        val blocks = listOf(
            RegexBlock(RegexBlockKind.START),
            RegexBlock(RegexBlockKind.CHARACTERS, set = RegexCharSet.DIGIT, quantifier = RegexQuantifier.RANGE, min = "2", max = "4"),
            RegexBlock(RegexBlockKind.LITERAL, text = "-"),
            RegexBlock(RegexBlockKind.ALTERNATION, text = "GET|POST"),
            RegexBlock(RegexBlockKind.GROUP, text = "\\w+", capture = false, quantifier = RegexQuantifier.OPTIONAL),
            RegexBlock(RegexBlockKind.BOUNDARY),
            RegexBlock(RegexBlockKind.END),
        )
        val pattern = RegexBuilder.pattern(blocks)
        assertEquals("^\\d{2,4}-(?:GET|POST)(?:\\w+)?\\b\$", pattern)
        assertTrue(Regex(pattern).matches("22-GET"))
        assertEquals("(\\w+)", RegexBuilder.render(RegexBlock(RegexBlockKind.GROUP, text = "\\w+")))
    }

    @Test
    fun formatsFlagsAsALiteral() {
        assertEquals("im", RegexBuilder.flags(ignoreCase = true, multiline = true))
        assertEquals("/a\\/b/i", RegexBuilder.literal("a/b", ignoreCase = true, multiline = false))
        assertEquals("/x/", RegexBuilder.literal("x", ignoreCase = false, multiline = false))
    }

    @Test
    fun everyPresetMatchesItsSampleAndIsDescribed() {
        RegexBuilder.presets.forEach { preset ->
            val titleEn = assertNotNull(preset.title.english(), "untranslated preset title")
            val titleRu = assertNotNull(preset.title.russian(), titleEn)
            val descriptionEn = assertNotNull(preset.description.english(), titleEn)
            val descriptionRu = assertNotNull(preset.description.russian(), titleEn)
            assertTrue(Regex(preset.pattern).containsMatchIn(preset.sample), titleEn)
            assertTrue(titleRu.isNotBlank() && descriptionEn.isNotBlank() && descriptionRu.isNotBlank(), titleEn)
        }
        assertEquals(RegexBuilder.presets.size, RegexBuilder.presets.map { it.title.english() }.toSet().size)
    }
}

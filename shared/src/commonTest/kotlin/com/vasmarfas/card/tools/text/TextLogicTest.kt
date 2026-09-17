package com.vasmarfas.card.tools.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CaseConvertTest {
    @Test
    fun splitsWordsOnBoundaries() {
        assertEquals(listOf("hello", "world"), CaseConvert.words("hello world"))
        assertEquals(listOf("XML", "Http", "Request"), CaseConvert.words("XMLHttpRequest"))
        assertEquals(listOf("some", "kebab", "case"), CaseConvert.words("some-kebab-case"))
    }

    @Test
    fun convertsLatin() {
        assertEquals("helloWorld", CaseConvert.convert("hello world", TextCase.CAMEL))
        assertEquals("HelloWorld", CaseConvert.convert("hello world", TextCase.PASCAL_CASE))
        assertEquals("hello_world", CaseConvert.convert("helloWorld", TextCase.SNAKE))
        assertEquals("HELLO_WORLD", CaseConvert.convert("hello world", TextCase.SCREAMING_SNAKE))
        assertEquals("hello-world", CaseConvert.convert("Hello World", TextCase.KEBAB))
        assertEquals("Hello World", CaseConvert.convert("hello world", TextCase.TITLE))
        assertEquals("Hello world. Bye now.", CaseConvert.convert("hello world. bye now.", TextCase.SENTENCE))
    }

    @Test
    fun handlesCyrillic() {
        assertEquals("ПРИВЕТ МИР", CaseConvert.convert("привет мир", TextCase.UPPER))
        assertEquals("Привет Мир", CaseConvert.convert("привет мир", TextCase.TITLE))
        assertEquals("привет_мир", CaseConvert.convert("Привет Мир", TextCase.SNAKE))
        assertEquals("пРИВЕТ", CaseConvert.convert("Привет", TextCase.INVERT))
    }

    @Test
    fun alternatingSkipsNonLetters() {
        assertEquals("aBc DeF", CaseConvert.convert("abc def", TextCase.ALTERNATING))
    }
}

class TextStatsTest {
    @Test
    fun countsBasics() {
        val stats = TextStats.analyze("Hello world.\n\nSecond paragraph here!")
        assertEquals(5, stats.words)
        assertEquals(2, stats.sentences)
        assertEquals(2, stats.paragraphs)
        assertEquals(3, stats.lines)
        assertEquals(5, stats.uniqueWords)
    }

    @Test
    fun countsBytesAndChars() {
        val stats = TextStats.analyze("дом abc")
        assertEquals(7, stats.chars)
        assertEquals(6, stats.charsNoSpaces)
        assertEquals(10, stats.utf8Bytes)
    }

    @Test
    fun topWordsAreSortedByCount() {
        val stats = TextStats.analyze("a b a c a b")
        assertEquals("a" to 3, stats.topWords[0])
        assertEquals("b" to 2, stats.topWords[1])
    }

    @Test
    fun emptyTextIsZero() {
        val stats = TextStats.analyze("")
        assertEquals(0, stats.words)
        assertEquals(0, stats.sentences)
        assertEquals(0, stats.paragraphs)
    }
}

class LineOpsTest {
    @Test
    fun sortsAndDedupes() {
        assertEquals("a\nb\nc", LineOps.apply("c\na\nb", LineOp.SORT_ASC))
        assertEquals("c\nb\na", LineOps.apply("c\na\nb", LineOp.SORT_DESC))
        assertEquals("a\nb", LineOps.apply("a\nb\na", LineOp.DEDUPE))
        assertEquals("a\nB", LineOps.apply("a\nB\nA", LineOp.DEDUPE, LineOptions(ignoreCase = true)))
    }

    @Test
    fun naturalSortOrdersNumbers() {
        assertEquals("item2\nitem10", LineOps.apply("item10\nitem2", LineOp.SORT_NATURAL))
        assertTrue(LineOps.compareNatural("a2", "a10") < 0)
        assertTrue(LineOps.compareNatural("a10", "a9") > 0)
    }

    @Test
    fun transformsLines() {
        assertEquals("b\na", LineOps.apply("a\nb", LineOp.REVERSE))
        assertEquals("a\nb", LineOps.apply(" a \n b ", LineOp.TRIM))
        assertEquals("a\nb", LineOps.apply("a\n\nb", LineOp.REMOVE_EMPTY))
        assertEquals("1. a\n2. b", LineOps.apply("a\nb", LineOp.NUMBER))
        assertEquals("<a>\n<b>", LineOps.apply("a\nb", LineOp.PREFIX_SUFFIX, LineOptions(prefix = "<", suffix = ">")))
        assertEquals("a, b", LineOps.apply("a\nb", LineOp.JOIN, LineOptions(separator = ", ")))
        assertEquals("a\nb", LineOps.apply("a, b", LineOp.SPLIT, LineOptions(separator = ", ")))
        assertEquals("a\nb", LineOps.apply("a\tb", LineOp.SPLIT, LineOptions(separator = "\\t")))
    }
}

class FindReplaceTest {
    @Test
    fun countsAndReplacesPlainText() {
        val result = FindReplace.run("Cat cat CAT", "cat", "dog", ignoreCase = true, useRegex = false, wholeWord = false)
        assertEquals(3, result.count)
        assertEquals("dog dog dog", result.output)
        assertNull(result.error)
    }

    @Test
    fun respectsCaseSensitivity() {
        val result = FindReplace.run("Cat cat", "cat", "dog", ignoreCase = false, useRegex = false, wholeWord = false)
        assertEquals(1, result.count)
        assertEquals("Cat dog", result.output)
    }

    @Test
    fun wholeWordSkipsSubstrings() {
        val result = FindReplace.run("cat category", "cat", "dog", ignoreCase = false, useRegex = false, wholeWord = true)
        assertEquals(1, result.count)
        assertEquals("dog category", result.output)
    }

    @Test
    fun regexGroupsExpandInReplacement() {
        val result = FindReplace.run("a1 b2", "([a-z])(\\d)", "$2$1", ignoreCase = false, useRegex = true, wholeWord = false)
        assertEquals("1a 2b", result.output)
        assertEquals(2, result.count)
    }

    @Test
    fun invalidRegexReportsError() {
        val result = FindReplace.run("x", "(", "", ignoreCase = false, useRegex = true, wholeWord = false)
        assertTrue(result.error != null)
        assertEquals(0, result.count)
    }
}

class TranslitTest {
    @Test
    fun passportScheme() {
        assertEquals("Ivanov", Translit.toLatin("Иванов", TranslitScheme.PASSPORT))
        assertEquals("Iuliia", Translit.toLatin("Юлия", TranslitScheme.PASSPORT))
        assertEquals("Shchukin", Translit.toLatin("Щукин", TranslitScheme.PASSPORT))
    }

    @Test
    fun gostScheme() {
        assertEquals("Yozhik", Translit.toLatin("Ёжик", TranslitScheme.GOST))
        assertEquals("xolod", Translit.toLatin("холод", TranslitScheme.GOST))
        assertEquals("cirk", Translit.toLatin("цирк", TranslitScheme.GOST))
    }

    @Test
    fun readableScheme() {
        assertEquals("Yozhik", Translit.toLatin("Ёжик", TranslitScheme.READABLE))
        assertEquals("kholod", Translit.toLatin("холод", TranslitScheme.READABLE))
        assertEquals("MOSKVA", Translit.toLatin("МОСКВА", TranslitScheme.READABLE))
    }

    @Test
    fun backToCyrillic() {
        assertEquals("холод", Translit.toCyrillic("kholod", TranslitScheme.READABLE))
        assertEquals("привет", Translit.toCyrillic("privet", TranslitScheme.READABLE))
    }

    @Test
    fun slugify() {
        assertEquals("privet-mir", Translit.slugify("Привет, мир!"))
        assertEquals("hello-world", Translit.slugify("Hello   World"))
        assertEquals("cafe-noir", Translit.slugify("Café Noir"))
    }
}

class MorseTest {
    @Test
    fun encodesLatinAndDigits() {
        assertEquals("... --- ...", Morse.encode("SOS"))
        assertEquals(".- -... / -.-. -..", Morse.encode("ab cd"))
        assertEquals("....- ..---", Morse.encode("42"))
    }

    @Test
    fun encodesCyrillic() {
        assertEquals("-- .. .-.", Morse.encode("мир"))
    }

    @Test
    fun decodesBothAlphabets() {
        assertEquals("sos", Morse.decode("... --- ...", MorseAlphabet.LATIN))
        assertEquals("мир", Morse.decode("-- .. .-.", MorseAlphabet.CYRILLIC))
        assertEquals("ab cd", Morse.decode(".- -... / -.-. -..", MorseAlphabet.LATIN))
    }

    @Test
    fun scheduleHasToneAndGaps() {
        val steps = Morse.schedule(".-")
        assertEquals(Morse.UNIT_MS, steps[0].ms)
        assertTrue(steps[0].tone)
        assertEquals(Morse.UNIT_MS * 3, steps[2].ms)
    }
}

class CiphersTest {
    @Test
    fun caesarShiftsAndRestores() {
        assertEquals("Khoor", Ciphers.caesar("Hello", 3))
        assertEquals("Hello", Ciphers.caesar(Ciphers.caesar("Hello", 3), -3))
        assertEquals("Тулезх", Ciphers.caesar("Привет", 3))
        assertEquals("Привет", Ciphers.caesar(Ciphers.caesar("Привет", 7), -7))
    }

    @Test
    fun rot13IsSelfInverse() {
        assertEquals("Uryyb", Ciphers.rot13("Hello"))
        assertEquals("Hello", Ciphers.rot13(Ciphers.rot13("Hello")))
    }

    @Test
    fun rot47IsSelfInverse() {
        assertEquals("w6==@", Ciphers.rot47("Hello"))
        assertEquals("Hello!", Ciphers.rot47(Ciphers.rot47("Hello!")))
    }

    @Test
    fun atbashMirrors() {
        assertEquals("Svool", Ciphers.atbash("Hello"))
        assertEquals("Hello", Ciphers.atbash(Ciphers.atbash("Hello")))
    }

    @Test
    fun vigenereRoundTrip() {
        val encrypted = Ciphers.vigenere("ATTACKATDAWN", "LEMON", decrypt = false)
        assertEquals("LXFOPVEFRNHR", encrypted)
        assertEquals("ATTACKATDAWN", Ciphers.vigenere(encrypted, "LEMON", decrypt = true))
    }

    @Test
    fun bruteForceContainsOriginal() {
        val encrypted = Ciphers.caesar("secret", 5)
        assertTrue(Ciphers.bruteForce(encrypted).any { it.second == "secret" })
    }
}

class TextDiffTest {
    @Test
    fun identicalTextsAreFullySimilar() {
        val result = TextDiff.diffLines("a\nb", "a\nb")
        assertEquals(0, result.added)
        assertEquals(0, result.removed)
        assertEquals(100.0, result.similarity, 0.001)
    }

    @Test
    fun detectsAddedAndRemoved() {
        val result = TextDiff.diffLines("a\nb\nc", "a\nx\nc")
        assertEquals(1, result.added)
        assertEquals(1, result.removed)
        assertEquals(listOf(DiffKind.EQUAL, DiffKind.REMOVED, DiffKind.ADDED, DiffKind.EQUAL), result.lines.map { it.kind })
    }

    @Test
    fun emptySideIsAllAdded() {
        val result = TextDiff.diffLines("", "a\nb")
        assertEquals(2, result.added)
        assertEquals(0, result.removed)
    }
}

class TextCleanerToolTest {
    @Test
    fun cleansWithSelectedOptions() {
        val text = "  <b>Hello</b>   world  "
        val output = cleanText(text, CleanOptions(stripHtml = true))
        assertEquals("Hello world", output)
    }

    @Test
    fun removesLineBreaksAndDiacritics() {
        assertEquals("a b", cleanText("a\nb", CleanOptions(removeLineBreaks = true)))
        assertEquals("Cafe", cleanText("Café", CleanOptions(removeDiacritics = true)))
        assertEquals("\"q\" - dash", cleanText("«q» — dash", CleanOptions(normalizePunctuation = true)))
    }
}

class UnicodeInfoTest {
    @Test
    fun describesLatinAndCyrillic() {
        val a = UnicodeInfo.info('A'.code)
        assertEquals("U+0041", a.hex)
        assertEquals("41", a.utf8)
        assertEquals("Basic Latin", a.block)
        assertEquals("Cyrillic", UnicodeInfo.info('Ж'.code).block)
        assertEquals("D0 96", UnicodeInfo.info('Ж'.code).utf8)
    }

    @Test
    fun handlesSurrogatePairs() {
        val emoji = UnicodeInfo.info(0x1F600)
        assertEquals("U+1F600", emoji.hex)
        assertEquals("F0 9F 98 80", emoji.utf8)
        assertEquals("D83D DE00", emoji.utf16)
        assertEquals(1, UnicodeInfo.inspect(emoji.text).size)
    }

    @Test
    fun escapesText() {
        assertEquals("\\u0416", UnicodeInfo.escapeUtf16("Ж"))
        assertEquals("&#x416;", UnicodeInfo.htmlHex("Ж"))
        assertEquals("&#1046;", UnicodeInfo.htmlDecimal("Ж"))
        assertEquals("%D0%96", UnicodeInfo.urlEncode("Ж"))
    }

    @Test
    fun parsesCodePointForms() {
        assertEquals(0x416, UnicodeInfo.parseCodePoint("U+0416"))
        assertEquals(0x416, UnicodeInfo.parseCodePoint("\\u0416"))
        assertEquals(0x416, UnicodeInfo.parseCodePoint("&#x416;"))
        assertEquals(1046, UnicodeInfo.parseCodePoint("1046"))
        assertEquals(0x416, UnicodeInfo.parseCodePoint("Ж"))
        assertNull(UnicodeInfo.parseCodePoint("not a code point"))
    }
}

class AsciiTableTest {
    @Test
    fun hasAllCodes() {
        assertEquals(128, AsciiTable.entries.size)
        val a = AsciiTable.entries[65]
        assertEquals("A", a.symbol)
        assertEquals("41", a.hex)
        assertEquals("101", a.oct)
        assertEquals("01000001", a.bin)
    }

    @Test
    fun searchFindsByCodeSymbolAndName() {
        assertTrue(AsciiTable.search("65", false).any { it.code == 65 })
        assertEquals("A", AsciiTable.search("0x41", false).single().symbol)
        assertTrue(AsciiTable.search("tilde", false).any { it.symbol == "~" })
        assertTrue(AsciiTable.search("", false).none { it.isControl })
        assertTrue(AsciiTable.search("", true).any { it.symbol == "ESC" })
    }
}

class LoremTest {
    @Test
    fun generatesRequestedAmount() {
        assertEquals(5, Lorem.generate(LoremUnit.WORDS, 5, LoremLang.LATIN, classicStart = false).split(" ").size)
        assertEquals(3, Lorem.generate(LoremUnit.PARAGRAPHS, 3, LoremLang.LATIN, classicStart = true).split("\n\n").size)
        assertTrue(Lorem.generate(LoremUnit.SENTENCES, 2, LoremLang.RUSSIAN, classicStart = true).isNotEmpty())
    }

    @Test
    fun classicStartIsUsed() {
        assertTrue(Lorem.generate(LoremUnit.SENTENCES, 1, LoremLang.LATIN, classicStart = true).startsWith("Lorem ipsum"))
        assertTrue(Lorem.generate(LoremUnit.PARAGRAPHS, 1, LoremLang.RUSSIAN, classicStart = true).startsWith("Далеко-далеко"))
    }

    @Test
    fun wordListsAreLargeEnough() {
        assertTrue(Lorem.latinWords.size >= 100)
        assertTrue(Lorem.russianWords.size >= 100)
    }
}

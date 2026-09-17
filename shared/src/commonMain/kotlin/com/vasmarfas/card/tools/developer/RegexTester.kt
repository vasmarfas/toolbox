package com.vasmarfas.card.tools.developer

import com.vasmarfas.card.core.dotMatchesAll
import com.vasmarfas.card.resources.*
import org.jetbrains.compose.resources.StringResource

data class RegexMatchInfo(
    val index: Int,
    val value: String,
    val start: Int,
    val end: Int,
    val groups: List<String?>,
)

data class RegexRunResult(
    val matches: List<RegexMatchInfo>,
    val error: String?,
    val replaced: String?,
)

object RegexTester {
    const val MAX_MATCHES = 500

    fun run(
        pattern: String,
        text: String,
        ignoreCase: Boolean,
        multiline: Boolean,
        dotAll: Boolean,
        replacement: String?,
    ): RegexRunResult {
        if (pattern.isEmpty()) return RegexRunResult(emptyList(), null, null)
        val options = buildSet {
            if (ignoreCase) add(RegexOption.IGNORE_CASE)
            if (multiline) add(RegexOption.MULTILINE)
            if (dotAll) add(dotMatchesAll)
        }
        val regex = try {
            Regex(pattern, options)
        } catch (e: Exception) {
            return RegexRunResult(emptyList(), e.message ?: "Invalid pattern", null)
        }
        return try {
            val matches = regex.findAll(text).take(MAX_MATCHES).mapIndexed { i, m ->
                RegexMatchInfo(i, m.value, m.range.first, m.range.last + 1, m.groups.drop(1).map { it?.value })
            }.toList()
            val replaced = replacement?.let { regex.replace(text, it) }
            RegexRunResult(matches, null, replaced)
        } catch (e: Exception) {
            RegexRunResult(emptyList(), e.message ?: "Invalid pattern", null)
        }
    }

    val cheatSheet: List<Pair<String, StringResource>> = listOf(
        "." to Res.string.any_character_except_newline,
        "\\d  \\D" to Res.string.digit_non_digit,
        "\\w  \\W" to Res.string.word_character_a_za_z0_9_other,
        "\\s  \\S" to Res.string.whitespace_non_whitespace,
        "\\b" to Res.string.word_boundary_2,
        "^  $" to Res.string.start_end_of_text_of_line_with_multiline,
        "[abc]  [^abc]" to Res.string.one_of_none_of_the_characters,
        "[a-z]" to Res.string.character_range,
        "a|b" to Res.string.alternation_2,
        "(…)" to Res.string.capturing_group_2,
        "(?:…)" to Res.string.non_capturing_group,
        "(?<name>…)" to Res.string.named_group,
        "*  +  ?" to Res.string.s_0_or_more_1_or_more_0_or_1,
        "{n}  {n,}  {n,m}" to Res.string.exactly_n_at_least_n_from_n_to_m,
        "*?  +?  ??" to Res.string.lazy_quantifiers,
        "(?=…)  (?!…)" to Res.string.positive_negative_lookahead,
        "(?<=…)  (?<!…)" to Res.string.positive_negative_lookbehind,
        "\\1  $1" to Res.string.back_reference_in_pattern_group_in_replaceme,
        "\\.  \\\\  \\(" to Res.string.escaped_special_characters,
        "\\t  \\n  \\r" to Res.string.tab_newline_carriage_return,
        "\\uFFFF" to Res.string.character_by_code,
    )
}

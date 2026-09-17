package com.vasmarfas.card.tools.developer

import com.vasmarfas.card.resources.*
import org.jetbrains.compose.resources.StringResource

enum class RegexBlockKind(val title: StringResource) {
    LITERAL(Res.string.text),
    CHARACTERS(Res.string.characters),
    GROUP(Res.string.group),
    ALTERNATION(Res.string.alternation),
    START(Res.string.start_anchor),
    END(Res.string.end_anchor),
    BOUNDARY(Res.string.word_boundary),
}

enum class RegexCharSet(val pattern: String, val title: StringResource) {
    DIGIT("\\d", Res.string.digit),
    LETTER("[A-Za-zА-Яа-яЁё]", Res.string.letter),
    LATIN("[A-Za-z]", Res.string.latin_letter),
    CYRILLIC("[А-Яа-яЁё]", Res.string.cyrillic_letter),
    WHITESPACE("\\s", Res.string.whitespace),
    WORD("\\w", Res.string.word_character),
    ANY(".", Res.string.any_character),
    CUSTOM("", Res.string.custom_set),
}

enum class RegexQuantifier(val title: StringResource) {
    ONCE(Res.string.once),
    OPTIONAL(Res.string.optional),
    ONE_OR_MORE(Res.string.one_or_more),
    ZERO_OR_MORE(Res.string.zero_or_more),
    EXACTLY(Res.string.exactly_n),
    RANGE(Res.string.from_n_to_m),
}

data class RegexBlock(
    val kind: RegexBlockKind,
    val text: String = "",
    val set: RegexCharSet = RegexCharSet.DIGIT,
    val quantifier: RegexQuantifier = RegexQuantifier.ONCE,
    val min: String = "2",
    val max: String = "5",
    val capture: Boolean = true,
)

data class RegexPreset(val title: StringResource, val pattern: String, val description: StringResource, val sample: String)

object RegexBuilder {
    private const val META = "\\^\$.|?*+()[]{}"

    val quantifiableKinds = setOf(RegexBlockKind.LITERAL, RegexBlockKind.CHARACTERS, RegexBlockKind.GROUP, RegexBlockKind.ALTERNATION)

    fun escape(text: String): String = buildString {
        text.forEach { char ->
            if (char in META) append('\\')
            append(char)
        }
    }

    fun escapeSet(text: String): String = buildString {
        text.forEach { char ->
            if (char == '\\' || char == ']' || char == '^') append('\\')
            append(char)
        }
    }

    fun suffix(block: RegexBlock): String {
        val min = block.min.trim().toIntOrNull() ?: 1
        val max = block.max.trim().toIntOrNull()
        return when (block.quantifier) {
            RegexQuantifier.ONCE -> ""
            RegexQuantifier.OPTIONAL -> "?"
            RegexQuantifier.ONE_OR_MORE -> "+"
            RegexQuantifier.ZERO_OR_MORE -> "*"
            RegexQuantifier.EXACTLY -> "{$min}"
            RegexQuantifier.RANGE -> if (max == null) "{$min,}" else "{$min,$max}"
        }
    }

    fun render(block: RegexBlock): String {
        val tail = suffix(block)
        return when (block.kind) {
            RegexBlockKind.LITERAL -> when {
                block.text.isEmpty() -> ""
                tail.isEmpty() || block.text.length == 1 -> escape(block.text) + tail
                else -> "(?:${escape(block.text)})$tail"
            }

            RegexBlockKind.CHARACTERS -> when {
                block.set != RegexCharSet.CUSTOM -> block.set.pattern + tail
                block.text.isEmpty() -> ""
                else -> "[${escapeSet(block.text)}]$tail"
            }

            RegexBlockKind.GROUP ->
                if (block.text.isEmpty()) "" else (if (block.capture) "(${block.text})" else "(?:${block.text})") + tail

            RegexBlockKind.ALTERNATION -> {
                val options = block.text.split('|').filter { it.isNotEmpty() }
                if (options.isEmpty()) "" else "(?:" + options.joinToString("|") { escape(it) } + ")" + tail
            }

            RegexBlockKind.START -> "^"
            RegexBlockKind.END -> "\$"
            RegexBlockKind.BOUNDARY -> "\\b"
        }
    }

    fun pattern(blocks: List<RegexBlock>): String = blocks.joinToString("") { render(it) }

    fun flags(ignoreCase: Boolean, multiline: Boolean): String =
        (if (ignoreCase) "i" else "") + (if (multiline) "m" else "")

    fun literal(pattern: String, ignoreCase: Boolean, multiline: Boolean): String =
        "/" + pattern.replace("/", "\\/") + "/" + flags(ignoreCase, multiline)

    val presets: List<RegexPreset> = listOf(
        RegexPreset(
            Res.string.email_4,
            "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}",
            Res.string.address_with_a_domain_of_at_least_two_letter,
            "ivan.petrov@example.com",
        ),
        RegexPreset(
            Res.string.url,
            "https?://[^\\s/\$.?#][^\\s]*",
            Res.string.http_or_https_link_up_to_the_first_whitespac,
            "https://example.com/page?id=1",
        ),
        RegexPreset(
            Res.string.ipv4,
            "\\b(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}\\b",
            Res.string.four_octets_of_0_to_255,
            "192.168.1.10",
        ),
        RegexPreset(
            Res.string.ipv6,
            "(([0-9A-Fa-f]{1,4}:){7}[0-9A-Fa-f]{1,4}|([0-9A-Fa-f]{1,4}:){1,6}:[0-9A-Fa-f]{1,4}|([0-9A-Fa-f]{1,4}:){1,5}(:[0-9A-Fa-f]{1,4}){1,2}|([0-9A-Fa-f]{1,4}:){1,4}(:[0-9A-Fa-f]{1,4}){1,3}|([0-9A-Fa-f]{1,4}:){1,3}(:[0-9A-Fa-f]{1,4}){1,4}|([0-9A-Fa-f]{1,4}:){1,2}(:[0-9A-Fa-f]{1,4}){1,5}|[0-9A-Fa-f]{1,4}:(:[0-9A-Fa-f]{1,4}){1,6}|([0-9A-Fa-f]{1,4}:){1,7}:|:((:[0-9A-Fa-f]{1,4}){1,7}|:))",
            Res.string.full_and_shortened_forms_with,
            "2001:db8::1",
        ),
        RegexPreset(
            Res.string.mac_address,
            "\\b[0-9A-Fa-f]{2}([:-][0-9A-Fa-f]{2}){5}\\b",
            Res.string.six_hex_pairs_separated_by_or,
            "3C:22:FB:0A:1D:9E",
        ),
        RegexPreset(
            Res.string.date_iso,
            "\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])",
            Res.string.year_month_day_month_and_day_are_range_check,
            "2024-02-29",
        ),
        RegexPreset(
            Res.string.date_dd_mm_yyyy,
            "(0[1-9]|[12]\\d|3[01])\\.(0[1-9]|1[0-2])\\.\\d{4}",
            Res.string.day_month_and_four_digit_year_with_dots,
            "29.02.2024",
        ),
        RegexPreset(
            Res.string.time,
            "([01]\\d|2[0-3]):[0-5]\\d(:[0-5]\\d)?",
            Res.string.s_24_hour_clock_seconds_optional,
            "23:59:07",
        ),
        RegexPreset(
            Res.string.russian_phone,
            "(\\+7|8)[ -]?\\(?\\d{3}\\)?[ -]?\\d{3}[ -]?\\d{2}[ -]?\\d{2}",
            Res.string.s_7_or_8_with_optional_brackets_spaces_and_das,
            "+7 (999) 123-45-67",
        ),
        RegexPreset(
            Res.string.international_phone,
            "\\+\\d{1,3}[ -]?\\d{2,4}[ -]?\\d{3,4}[ -]?\\d{2,4}",
            Res.string.country_code_and_groups_of_digits_in_e_164_s,
            "+44 20 7946 0958",
        ),
        RegexPreset(
            Res.string.hex_colour,
            "#([0-9A-Fa-f]{6}|[0-9A-Fa-f]{3})\\b",
            Res.string.six_or_three_hex_digits_after,
            "#1E88E5",
        ),
        RegexPreset(
            Res.string.uuid,
            "[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}",
            Res.string.canonical_8_4_4_4_12_form_of_any_version,
            "123e4567-e89b-12d3-a456-426614174000",
        ),
        RegexPreset(
            Res.string.jwt,
            "eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+",
            Res.string.three_base64url_parts_header_starts_with_eyj,
            "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.SflKxwRJSM",
        ),
        RegexPreset(
            Res.string.domain,
            "\\b([A-Za-z0-9](-*[A-Za-z0-9])*\\.)+[A-Za-z]{2,}\\b",
            Res.string.name_with_any_number_of_levels_and_a_letter,
            "sub.example.co.uk",
        ),
        RegexPreset(
            Res.string.username,
            "\\b[A-Za-z][A-Za-z0-9_.-]{2,19}\\b",
            Res.string.starts_with_a_letter_3_to_20_characters,
            "john_doe",
        ),
        RegexPreset(
            Res.string.password_strength,
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}\$",
            Res.string.at_least_8_characters_with_lower_and_upper_c,
            "Str0ng!pass",
        ),
        RegexPreset(
            Res.string.integer,
            "-?\\d+",
            Res.string.digits_with_an_optional_minus,
            "-42",
        ),
        RegexPreset(
            Res.string.decimal_2,
            "-?\\d+([.,]\\d+)?",
            Res.string.fractional_part_after_a_dot_or_a_comma,
            "3.14",
        ),
        RegexPreset(
            Res.string.document_number,
            "\\b\\d{4} ?\\d{6}\\b",
            Res.string.four_digits_of_the_series_and_six_of_the_num,
            "4509 123456",
        ),
    )
}

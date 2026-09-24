package com.vasmarfas.card.tools.everyday

import com.vasmarfas.card.tools.design.Ean13

enum class CardBrand(val lengths: Set<Int>) {
    VISA(setOf(13, 16, 19)),
    MASTERCARD(setOf(16)),
    MIR((16..19).toSet()),
    AMEX(setOf(15)),
    UNIONPAY((16..19).toSet()),
    JCB((16..19).toSet()),
    DINERS((14..19).toSet()),
    DISCOVER((16..19).toSet()),
    MAESTRO((12..19).toSet()),
    UZCARD(setOf(16)),
    HUMO(setOf(16)),
    BELKART(setOf(16)),
}

enum class OgrnRecord { OGRN, GRN, OGRNIP, GRNIP }

enum class AccountType(val prefix: String) {
    CORRESPONDENT("30101"),
    COMMERCIAL("40702"),
    NON_PROFIT("40703"),
    ENTREPRENEUR("40802"),
    NON_RESIDENT_COMPANY("40807"),
    PERSON("40817"),
    NON_RESIDENT_PERSON("40820"),
    DEPOSIT("423"),
}

enum class GtinFormat { EAN8, UPCA, EAN13, GTIN14 }

enum class Gs1Use { COUNTRY, RESTRICTED, OFFICE, ISSN, REFUND, COUPON }

class Gs1Issuer(val use: Gs1Use, val countries: List<String> = emptyList())

enum class VinRegion { AFRICA, ASIA, EUROPE, NORTH_AMERICA, OCEANIA, SOUTH_AMERICA }

// expected is the check digits the number would need, null when they match
sealed class IdCheck(val valid: Boolean, val formatted: String, val expected: String?) {
    val checkDigits: Int get() = if (this is IbanCheck || this is SnilsCheck || this is InnCheck && !organization) 2 else 1
}

class CardCheck(valid: Boolean, formatted: String, expected: String?, val brand: CardBrand?, val lengthFits: Boolean) :
    IdCheck(valid, formatted, expected)

class IbanCheck(valid: Boolean, formatted: String, expected: String?, val country: String, val length: Int, val countryLength: Int?) :
    IdCheck(valid, formatted, expected)

class InnCheck(valid: Boolean, formatted: String, expected: String?, val organization: Boolean, val region: String, val taxOffice: String) :
    IdCheck(valid, formatted, expected)

class SnilsCheck(valid: Boolean, formatted: String, expected: String?, val checked: Boolean) :
    IdCheck(valid, formatted, expected)

class OgrnCheck(valid: Boolean, formatted: String, expected: String?, val record: OgrnRecord?, val year: Int, val region: String) :
    IdCheck(valid, formatted, expected)

class AccountCheck(valid: Boolean, formatted: String, expected: String?, val currency: String, val type: AccountType?, val bikFits: Boolean) :
    IdCheck(valid, formatted, expected)

class ImeiCheck(valid: Boolean, formatted: String, expected: String?, val tac: String) :
    IdCheck(valid, formatted, expected)

class IsbnCheck(valid: Boolean, formatted: String, expected: String?, val other: String?) :
    IdCheck(valid, formatted, expected)

class GtinCheck(valid: Boolean, formatted: String, expected: String?, val format: GtinFormat, val issuer: Gs1Issuer?) :
    IdCheck(valid, formatted, expected)

// the check digit is binding only in North America, elsewhere a mismatch leaves the VIN valid
class VinCheck(valid: Boolean, formatted: String, expected: String?, val region: VinRegion?, val modelYears: List<Int>) :
    IdCheck(valid, formatted, expected)

object Identifiers {
    private val ibanLengths = mapOf(
        "AD" to 24, "AE" to 23, "AL" to 28, "AT" to 20, "AZ" to 28, "BA" to 20, "BE" to 16, "BG" to 22, "BH" to 22,
        "BI" to 27, "BR" to 29, "BY" to 28, "CH" to 21, "CR" to 22, "CY" to 28, "CZ" to 24, "DE" to 22, "DJ" to 27,
        "DK" to 18, "DO" to 28, "EE" to 20, "EG" to 29, "ES" to 24, "FI" to 18, "FK" to 18, "FO" to 18, "FR" to 27,
        "GB" to 22, "GE" to 22, "GI" to 23, "GL" to 18, "GR" to 27, "GT" to 28, "HN" to 28, "HR" to 21, "HU" to 28,
        "IE" to 22, "IL" to 23, "IQ" to 23, "IS" to 26, "IT" to 27, "JO" to 30, "KW" to 30, "KZ" to 20, "LB" to 28,
        "LC" to 32, "LI" to 21, "LT" to 20, "LU" to 20, "LV" to 21, "LY" to 25, "MC" to 27, "MD" to 24, "ME" to 22,
        "MK" to 19, "MN" to 20, "MR" to 27, "MT" to 31, "MU" to 30, "NI" to 28, "NL" to 18, "NO" to 15, "OM" to 23,
        "PK" to 24, "PL" to 28, "PS" to 29, "PT" to 25, "QA" to 29, "RO" to 24, "RS" to 22, "RU" to 33, "SA" to 24,
        "SC" to 31, "SD" to 18, "SE" to 24, "SI" to 19, "SK" to 24, "SM" to 27, "SO" to 23, "ST" to 25, "SV" to 28,
        "TL" to 23, "TN" to 24, "TR" to 26, "UA" to 29, "VA" to 22, "VG" to 24, "XK" to 20, "YE" to 30,
    )

    private val maestroPrefixes = setOf(5018, 5020, 5038, 5893, 6304, 6759, 6761, 6762, 6763)

    private val innWeights10 = intArrayOf(2, 4, 10, 3, 5, 9, 4, 6, 8)
    private val innWeights11 = intArrayOf(7, 2, 4, 10, 3, 5, 9, 4, 6, 8)
    private val innWeights12 = intArrayOf(3, 7, 2, 4, 10, 3, 5, 9, 4, 6, 8)
    private val accountWeights = intArrayOf(7, 1, 3)
    private val vinWeights = intArrayOf(8, 7, 6, 5, 4, 3, 2, 10, 0, 9, 8, 7, 6, 5, 4, 3, 2)

    private const val VIN_ALPHABET = "0123456789ABCDEFGHJKLMNPRSTUVWXYZ"
    private const val VIN_YEARS = "ABCDEFGHJKLMNPRSTVWXY123456789"

    private val restricted = Gs1Issuer(Gs1Use.RESTRICTED)

    private fun country(vararg codes: String) = Gs1Issuer(Gs1Use.COUNTRY, codes.toList())

    private val gs1Prefixes: List<Pair<IntRange, Gs1Issuer>> = listOf(
        1..19 to country("US"), 20..29 to restricted, 30..39 to country("US"), 40..49 to restricted,
        60..139 to country("US"), 200..299 to restricted, 300..379 to country("FR", "MC"), 380..380 to country("BG"),
        381..381 to country("XK"), 383..383 to country("SI"), 385..385 to country("HR"), 387..387 to country("BA"),
        389..389 to country("ME"), 400..440 to country("DE"), 450..459 to country("JP"), 460..469 to country("RU"),
        470..470 to country("KG"), 471..471 to country("TW"), 474..474 to country("EE"), 475..475 to country("LV"),
        476..476 to country("AZ"), 477..477 to country("LT"), 478..478 to country("UZ"), 479..479 to country("LK"),
        480..480 to country("PH"), 481..481 to country("BY"), 482..482 to country("UA"), 483..483 to country("TM"),
        484..484 to country("MD"), 485..485 to country("AM"), 486..486 to country("GE"), 487..487 to country("KZ"),
        488..488 to country("TJ"), 489..489 to country("HK"), 490..499 to country("JP"), 500..509 to country("GB"),
        520..521 to country("GR"), 528..528 to country("LB"), 529..529 to country("CY"), 530..530 to country("AL"),
        531..531 to country("MK"), 535..535 to country("MT"), 539..539 to country("IE"), 540..549 to country("BE", "LU"),
        560..560 to country("PT"), 569..569 to country("IS"), 570..579 to country("DK", "FO", "GL"), 590..590 to country("PL"),
        594..594 to country("RO"), 599..599 to country("HU"), 600..601 to country("ZA"), 603..603 to country("GH"),
        604..604 to country("SN"), 605..605 to country("UG"), 606..606 to country("AO"), 607..607 to country("OM"),
        608..608 to country("BH"), 609..609 to country("MU"), 611..611 to country("MA"), 612..612 to country("SO"),
        613..613 to country("DZ"), 615..615 to country("NG"), 616..616 to country("KE"), 617..617 to country("CM"),
        618..618 to country("CI"), 619..619 to country("TN"), 620..620 to country("TZ"), 621..621 to country("SY"),
        622..622 to country("EG"), 624..624 to country("LY"), 625..625 to country("JO"), 626..626 to country("IR"),
        627..627 to country("KW"), 628..628 to country("SA"), 629..629 to country("AE"), 630..630 to country("QA"),
        631..631 to country("NA"), 632..632 to country("RW"), 640..649 to country("FI"), 680..681 to country("CN"),
        690..699 to country("CN"), 700..709 to country("NO"), 729..729 to country("IL"), 730..739 to country("SE"),
        740..740 to country("GT"), 741..741 to country("SV"), 742..742 to country("HN"), 743..743 to country("NI"),
        744..744 to country("CR"), 745..745 to country("PA"), 746..746 to country("DO"), 750..750 to country("MX"),
        754..755 to country("CA"), 759..759 to country("VE"), 760..769 to country("CH", "LI"), 770..771 to country("CO"),
        773..773 to country("UY"), 775..775 to country("PE"), 777..777 to country("BO"), 778..779 to country("AR"),
        780..780 to country("CL"), 784..784 to country("PY"), 786..786 to country("EC"), 789..790 to country("BR"),
        800..839 to country("IT", "SM", "VA"), 840..849 to country("ES", "AD"), 850..850 to country("CU"),
        858..858 to country("SK"), 859..859 to country("CZ"), 860..860 to country("RS"), 865..865 to country("MN"),
        867..867 to country("KP"), 868..869 to country("TR"), 870..879 to country("NL"), 880..881 to country("KR"),
        883..883 to country("MM"), 884..884 to country("KH"), 885..885 to country("TH"), 887..887 to country("LA"),
        888..888 to country("SG"), 890..890 to country("IN"), 893..893 to country("VN"), 894..894 to country("BD"),
        896..896 to country("PK"), 899..899 to country("ID"), 900..919 to country("AT"), 930..939 to country("AU"),
        940..949 to country("NZ"), 950..952 to Gs1Issuer(Gs1Use.OFFICE), 955..955 to country("MY"), 958..958 to country("MO"),
        977..977 to Gs1Issuer(Gs1Use.ISSN), 980..980 to Gs1Issuer(Gs1Use.REFUND), 981..983 to Gs1Issuer(Gs1Use.COUPON),
        990..999 to Gs1Issuer(Gs1Use.COUPON),
    )

    fun check(input: String, bik: String, currentYear: Int): List<IdCheck> {
        val s = input.filter { it.isLetterOrDigit() }.uppercase()
        if (s.isEmpty()) return emptyList()
        val found = mutableListOf<IdCheck>()
        if (s.all { it in '0'..'9' }) {
            card(s)?.let(found::add)
            when (s.length) {
                10, 12 -> found += inn(s)
                11 -> found += snils(s)
                13 -> found += ogrn(s, currentYear)
                15 -> {
                    found += imei(s)
                    found += ogrn(s, currentYear)
                }
                20 -> found += account(s, bik)
            }
            isbn(s)?.let(found::add)
            gtin(s)?.let(found::add)
        } else {
            iban(s)?.let(found::add)
            vin(s, currentYear)?.let(found::add)
            isbn(s)?.let(found::add)
        }
        return found.sortedByDescending { it.valid }
    }

    private fun luhnDigit(payload: String): Int {
        var sum = 0
        payload.reversed().forEachIndexed { i, c ->
            var v = c - '0'
            if (i % 2 == 0) {
                v *= 2
                if (v > 9) v -= 9
            }
            sum += v
        }
        return (10 - sum % 10) % 10
    }

    private fun cardBrand(digits: String): CardBrand? {
        val two = digits.take(2).toInt()
        val three = digits.take(3).toInt()
        val four = digits.take(4).toInt()
        return when {
            four in 2200..2204 -> CardBrand.MIR
            four in 2221..2720 || two in 51..55 -> CardBrand.MASTERCARD
            digits[0] == '4' -> CardBrand.VISA
            two == 34 || two == 37 -> CardBrand.AMEX
            four in 3528..3589 -> CardBrand.JCB
            three in 300..305 || three == 309 || two == 36 || two == 38 || two == 39 -> CardBrand.DINERS
            four == 6011 || three in 644..649 || two == 65 -> CardBrand.DISCOVER
            two == 62 -> CardBrand.UNIONPAY
            four == 8600 -> CardBrand.UZCARD
            four == 9860 -> CardBrand.HUMO
            four == 9112 -> CardBrand.BELKART
            four in maestroPrefixes -> CardBrand.MAESTRO
            else -> null
        }
    }

    private fun card(d: String): CardCheck? {
        if (d.length !in 12..19) return null
        val brand = cardBrand(d)
        val lengthFits = brand == null || d.length in brand.lengths
        if (d.length < 16 && (brand == null || !lengthFits)) return null
        val check = luhnDigit(d.dropLast(1))
        val luhn = check == d.last() - '0'
        if (d.length < 16 && !luhn && brand != CardBrand.AMEX && brand != CardBrand.DINERS) return null
        val formatted = if (d.length in 14..15 && (brand == CardBrand.AMEX || brand == CardBrand.DINERS)) {
            "${d.take(4)} ${d.substring(4, 10)} ${d.substring(10)}"
        } else {
            d.chunked(4).joinToString(" ")
        }
        return CardCheck(luhn && lengthFits, formatted, check.toString().takeIf { !luhn }, brand, lengthFits)
    }

    private fun mod97(s: String): Int {
        var r = 0
        for (c in s) {
            r = if (c in '0'..'9') (r * 10 + (c - '0')) % 97 else (r * 100 + (c - 'A' + 10)) % 97
        }
        return r
    }

    private fun iban(s: String): IbanCheck? {
        if (s.length !in 15..34 || s[0] !in 'A'..'Z' || s[1] !in 'A'..'Z' || s[2] !in '0'..'9' || s[3] !in '0'..'9') return null
        if (!s.all { it in 'A'..'Z' || it in '0'..'9' }) return null
        val country = s.take(2)
        val countryLength = ibanLengths[country]
        val bban = s.drop(4)
        val checksum = mod97(bban + country + s.substring(2, 4)) == 1
        val expected = (98 - mod97(bban + country + "00")).toString().padStart(2, '0')
        return IbanCheck(
            valid = checksum && countryLength == s.length,
            formatted = s.chunked(4).joinToString(" "),
            expected = expected.takeIf { !checksum },
            country = country,
            length = s.length,
            countryLength = countryLength,
        )
    }

    private fun weighted(digits: String, weights: IntArray): Int = weights.indices.sumOf { (digits[it] - '0') * weights[it] } % 11 % 10

    private fun inn(d: String): InnCheck {
        val expected = if (d.length == 10) {
            weighted(d, innWeights10).toString()
        } else {
            val first = weighted(d, innWeights11)
            "$first${weighted(d.take(10) + first, innWeights12)}"
        }
        val matches = d.takeLast(expected.length) == expected
        val region = d.take(2)
        return InnCheck(matches && region != "00", d, expected.takeIf { !matches }, d.length == 10, region, d.take(4))
    }

    private fun snils(d: String): SnilsCheck {
        val sum = (0 until 9).sumOf { (d[it] - '0') * (9 - it) }
        val control = when {
            sum < 100 -> sum
            sum < 102 -> 0
            else -> sum % 101 % 100
        }
        val expected = control.toString().padStart(2, '0')
        val checked = d.take(9).toInt() > 1_001_998
        val matches = !checked || d.takeLast(2) == expected
        val formatted = "${d.take(3)}-${d.substring(3, 6)}-${d.substring(6, 9)} ${d.takeLast(2)}"
        return SnilsCheck(matches, formatted, expected.takeIf { !matches }, checked)
    }

    private fun ogrn(d: String, currentYear: Int): OgrnCheck {
        val body = d.dropLast(1).toLong()
        val control = ((if (d.length == 13) body % 11 else body % 13) % 10).toInt()
        val record = if (d.length == 13) {
            when (d[0]) {
                '1', '5' -> OgrnRecord.OGRN
                '2', '6', '7', '8', '9' -> OgrnRecord.GRN
                else -> null
            }
        } else {
            when (d[0]) {
                '3' -> OgrnRecord.OGRNIP
                '4' -> OgrnRecord.GRNIP
                else -> null
            }
        }
        val year = 2000 + d.substring(1, 3).toInt()
        val matches = control == d.last() - '0'
        return OgrnCheck(matches && record != null && year <= currentYear, d, control.toString().takeIf { !matches }, record, year, d.substring(3, 5))
    }

    private fun accountSum(digits: String): Int = digits.indices.sumOf { (digits[it] - '0') * accountWeights[it % 3] % 10 }

    private fun account(d: String, bik: String): AccountCheck {
        val formatted = "${d.take(5)} ${d.substring(5, 8)} ${d[8]} ${d.substring(9, 13)} ${d.substring(13)}"
        val currency = d.substring(5, 8)
        val type = AccountType.entries.firstOrNull { d.startsWith(it.prefix) }
        val b = bik.filter { it in '0'..'9' }
        if (b.length != 9) return AccountCheck(false, formatted, null, currency, type, bikFits = false)
        val key = if (d.startsWith(AccountType.CORRESPONDENT.prefix)) "0" + b.substring(4, 6) else b.takeLast(3)
        val control = accountSum(key + d.take(8) + "0" + d.drop(9)) % 10 * 3 % 10
        val matches = control == d[8] - '0'
        return AccountCheck(matches, formatted, control.toString().takeIf { !matches }, currency, type, bikFits = true)
    }

    private fun imei(d: String): ImeiCheck {
        val check = luhnDigit(d.dropLast(1))
        val matches = check == d.last() - '0'
        return ImeiCheck(matches, "${d.take(8)} ${d.substring(8, 14)} ${d.last()}", check.toString().takeIf { !matches }, d.take(8))
    }

    private fun isbn10Digit(nine: String): Char {
        val c = (11 - (0 until 9).sumOf { (nine[it] - '0') * (10 - it) } % 11) % 11
        return if (c == 10) 'X' else '0' + c
    }

    private fun isbn(s: String): IsbnCheck? {
        if (s.length == 10 && s.take(9).all { it in '0'..'9' } && (s[9] in '0'..'9' || s[9] == 'X')) {
            val check = isbn10Digit(s.take(9))
            val matches = check == s[9]
            return IsbnCheck(matches, s, check.toString().takeIf { !matches }, Ean13.complete("978" + s.take(9)).takeIf { matches })
        }
        if (s.length != 13 || !(s.startsWith("978") || s.startsWith("979")) || s.any { it !in '0'..'9' }) return null
        val check = Ean13.checksum(s.dropLast(1)) ?: return null
        val matches = check == s.last() - '0'
        val other = if (matches && s.startsWith("978")) s.substring(3, 12) + isbn10Digit(s.substring(3, 12)) else null
        return IsbnCheck(matches, s, check.toString().takeIf { !matches }, other)
    }

    private fun gs1Issuer(prefix: Int): Gs1Issuer? = gs1Prefixes.firstOrNull { prefix in it.first }?.second

    private fun gtin(d: String): GtinCheck? {
        val format = when (d.length) {
            8 -> GtinFormat.EAN8
            12 -> GtinFormat.UPCA
            13 -> GtinFormat.EAN13
            14 -> GtinFormat.GTIN14
            else -> return null
        }
        if (format == GtinFormat.EAN13 && (d.startsWith("978") || d.startsWith("979"))) return null
        val check = Ean13.checksum(d.dropLast(1)) ?: return null
        val matches = check == d.last() - '0'
        val issuer = when (format) {
            GtinFormat.EAN8 -> if (d[0] == '0' || d[0] == '2') restricted else gs1Issuer(d.take(3).toInt())
            GtinFormat.UPCA -> gs1Issuer(d.take(2).toInt())
            GtinFormat.EAN13 -> gs1Issuer(d.take(3).toInt())
            GtinFormat.GTIN14 -> gs1Issuer(d.substring(1, 4).toInt())
        }
        return GtinCheck(matches, d, check.toString().takeIf { !matches }, format, issuer)
    }

    private fun vinValue(c: Char): Int = when (c) {
        in '0'..'9' -> c - '0'
        'A', 'J' -> 1
        'B', 'K', 'S' -> 2
        'C', 'L', 'T' -> 3
        'D', 'M', 'U' -> 4
        'E', 'N', 'V' -> 5
        'F', 'W' -> 6
        'G', 'P', 'X' -> 7
        'H', 'Y' -> 8
        else -> 9
    }

    private fun vin(s: String, currentYear: Int): VinCheck? {
        if (s.length != 17 || s.any { it !in VIN_ALPHABET } || s.all { it in '0'..'9' }) return null
        val sum = s.indices.sumOf { vinValue(s[it]) * vinWeights[it] } % 11
        val check = if (sum == 10) 'X' else '0' + sum
        val matches = check == s[8]
        val region = when (s[0]) {
            in 'A'..'H' -> VinRegion.AFRICA
            in 'J'..'R' -> VinRegion.ASIA
            in 'S'..'Z' -> VinRegion.EUROPE
            in '1'..'5' -> VinRegion.NORTH_AMERICA
            '6', '7' -> VinRegion.OCEANIA
            '8', '9' -> VinRegion.SOUTH_AMERICA
            else -> null
        }
        val yearIndex = VIN_YEARS.indexOf(s[9])
        val years = if (yearIndex < 0) emptyList() else generateSequence(1980 + yearIndex) { it + 30 }.takeWhile { it <= currentYear + 1 }.toList()
        val formatted = "${s.take(3)} ${s.substring(3, 9)} ${s.substring(9)}"
        return VinCheck(matches || region != VinRegion.NORTH_AMERICA, formatted, check.toString().takeIf { !matches }, region, years)
    }
}

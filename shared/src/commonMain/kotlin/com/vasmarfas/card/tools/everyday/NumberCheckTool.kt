package com.vasmarfas.card.tools.everyday

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.LocalLang
import com.vasmarfas.card.core.regionName
import com.vasmarfas.card.core.str
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.tools.money.Currency
import com.vasmarfas.card.tools.time.today
import com.vasmarfas.card.ui.components.ChoiceChips
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.ToolInputField
import com.vasmarfas.card.ui.components.ToolSection
import com.vasmarfas.card.ui.theme.LocalStatusColors
import org.jetbrains.compose.resources.stringResource

val numberCheckTool = Tool(
    id = "number-check",
    category = ToolCategory.EVERYDAY,
    title = Res.string.number_check,
    description = Res.string.number_check_description,
    icon = Icons.Filled.Pin,
    keywords = listOf(
        "card", "luhn", "iban", "inn", "snils", "ogrn", "ogrnip", "bik", "bank account", "imei", "isbn", "ean", "upc",
        "gtin", "barcode", "vin", "check digit", "validator",
        "карта", "луна", "инн", "снилс", "огрн", "огрнип", "бик", "расчётный счёт", "счёт", "имей", "штрихкод",
        "вин", "реквизиты", "контрольная цифра", "проверка",
    ),
) { NumberCheckScreen() }

private class Example(val number: String, val bik: String = "")

private val examples = listOf(
    Example("4111 1111 1111 1111"),
    Example("DE89 3704 0044 0532 0130 00"),
    Example("7707083893"),
    Example("112-233-445 95"),
    Example("1027700132195"),
    Example("30101810400000000225", "044525225"),
    Example("490154203237518"),
    Example("978-0-306-40615-7"),
    Example("4601234567893"),
    Example("1M8GDM9AXKP042788"),
)

private val currencyCodes = mapOf(
    "810" to "RUB", "643" to "RUB", "840" to "USD", "978" to "EUR", "156" to "CNY", "398" to "KZT", "933" to "BYN",
    "826" to "GBP", "756" to "CHF", "392" to "JPY", "949" to "TRY", "784" to "AED", "051" to "AMD", "860" to "UZS",
    "417" to "KGS", "981" to "GEL", "980" to "UAH", "356" to "INR",
)

@Composable
private fun NumberCheckScreen() {
    var number by rememberSaveable { mutableStateOf("") }
    var bik by rememberSaveable { mutableStateOf("") }
    val year = remember { today().year }
    val checks = remember(number, bik) { Identifiers.check(number, bik, year) }
    ToolInputField(
        value = number,
        onValueChange = { number = it },
        label = Res.string.number_to_check.str(),
        keyboardType = KeyboardType.Ascii,
        monospace = true,
        trailingIcon = if (number.isEmpty()) {
            null
        } else {
            {
                IconButton(onClick = { number = "" }) {
                    Icon(Icons.Filled.Clear, contentDescription = Res.string.clear.str())
                }
            }
        },
    )
    if (checks.any { it is AccountCheck }) {
        ToolInputField(
            value = bik,
            onValueChange = { bik = it },
            label = Res.string.bank_bik.str(),
            keyboardType = KeyboardType.Number,
            monospace = true,
            supportingText = Res.string.account_key_needs_bik.str(),
        )
    }
    if (number.isBlank()) {
        Text(
            Res.string.number_check_supported.str(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ToolSection(Res.string.examples.str()) {
            val samples = remember(year) { examples.map { it to Identifiers.check(it.number, it.bik, year).first() } }
            ChoiceChips(
                options = samples,
                selected = null,
                onSelect = { (example, _) ->
                    number = example.number
                    bik = example.bik
                },
                label = { checkTitle(it.second) },
            )
        }
        return
    }
    if (checks.isEmpty()) {
        ResultCard {
            Text(Res.string.number_not_recognised.str(), style = MaterialTheme.typography.titleMedium)
            Text(
                Res.string.number_check_supported.str(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    checks.forEach { CheckCard(it) }
}

@Composable
private fun CheckCard(check: IdCheck) {
    val status = LocalStatusColors.current
    val pending = check is AccountCheck && !check.bikFits
    ResultCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                when {
                    pending -> Icons.AutoMirrored.Filled.HelpOutline
                    check.valid -> Icons.Filled.CheckCircle
                    else -> Icons.Filled.Cancel
                },
                contentDescription = null,
                tint = when {
                    pending -> status.warn
                    check.valid -> status.good
                    else -> status.bad
                },
            )
            Column(Modifier.weight(1f)) {
                Text(checkTitle(check), style = MaterialTheme.typography.titleMedium)
                Text(verdict(check), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        KeyValueRow(Res.string.number_to_check.str(), check.formatted, copyValue = check.formatted.filter { it != ' ' && it != '-' })
        check.expected?.let {
            KeyValueRow(if (check.checkDigits == 1) Res.string.expected_check_digit.str() else Res.string.expected_check_digits.str(), it, copyable = false)
        }
        CheckFacts(check)
    }
}

@Composable
private fun checkTitle(check: IdCheck): String = when (check) {
    is CardCheck -> Res.string.bank_card.str()
    is IbanCheck -> "IBAN"
    is InnCheck -> if (check.organization) Res.string.inn_of_organisation.str() else Res.string.inn_of_person.str()
    is SnilsCheck -> Res.string.snils.str()
    is OgrnCheck -> if (check.formatted.length == 13) Res.string.ogrn.str() else Res.string.ogrnip.str()
    is AccountCheck -> Res.string.bank_account.str()
    is ImeiCheck -> "IMEI"
    is IsbnCheck -> "ISBN"
    is GtinCheck -> when (check.format) {
        GtinFormat.EAN8 -> "EAN-8"
        GtinFormat.UPCA -> "UPC-A"
        GtinFormat.EAN13 -> "EAN-13"
        GtinFormat.GTIN14 -> "GTIN-14"
    }
    is VinCheck -> "VIN"
}

@Composable
private fun verdict(check: IdCheck): String {
    val lang = LocalLang.current
    return when {
        check is AccountCheck && !check.bikFits -> Res.string.enter_bik_to_check_account.str()
        check.expected != null && check is VinCheck && check.valid -> Res.string.vin_check_digit_optional.str()
        check.expected != null -> if (check.checkDigits == 1) Res.string.check_digit_does_not_match.str() else Res.string.check_digits_do_not_match.str()
        check is CardCheck && check.brand != null && !check.lengthFits -> stringResource(Res.string.card_length_does_not_fit, brandName(check.brand))
        check is IbanCheck && check.countryLength == null ->
            stringResource(Res.string.country_does_not_use_iban, regionName(check.country, lang) ?: check.country)
        check is IbanCheck && check.countryLength != null && !check.valid -> stringResource(Res.string.iban_length_for_country, check.countryLength, check.length)
        check is InnCheck && !check.valid -> Res.string.region_code_00_does_not_exist.str()
        check is OgrnCheck && check.record == null -> Res.string.ogrn_cannot_start_with_this_digit.str()
        check is OgrnCheck && !check.valid -> stringResource(Res.string.record_year_has_not_come, check.year)
        check is SnilsCheck && !check.checked -> Res.string.snils_without_check_digits.str()
        else -> if (check.checkDigits == 1) Res.string.check_digit_matches.str() else Res.string.check_digits_match.str()
    }
}

@Composable
private fun CheckFacts(check: IdCheck) {
    val lang = LocalLang.current
    when (check) {
        is CardCheck -> check.brand?.let { KeyValueRow(Res.string.payment_system.str(), brandName(it), mono = false, copyable = false) }
        is IbanCheck -> KeyValueRow(Res.string.country.str(), regionName(check.country, lang) ?: check.country, mono = false, copyable = false)
        is InnCheck -> {
            KeyValueRow(Res.string.region_code.str(), check.region, copyable = false)
            KeyValueRow(Res.string.tax_office_code.str(), check.taxOffice, copyable = false)
        }
        is SnilsCheck -> Unit
        is OgrnCheck -> {
            check.record?.let { KeyValueRow(Res.string.record_type.str(), recordName(it), mono = false, copyable = false) }
            KeyValueRow(Res.string.record_year.str(), check.year.toString(), copyable = false)
            KeyValueRow(Res.string.region_code.str(), check.region, copyable = false)
        }
        is AccountCheck -> {
            val currency = currencyCodes[check.currency]?.let { Currency.names[it] }?.str()
            KeyValueRow(Res.string.currency.str(), currency?.let { "$it (${check.currency})" } ?: check.currency, mono = false, copyable = false)
            check.type?.let { KeyValueRow(Res.string.account_type.str(), accountTypeName(it), mono = false, copyable = false) }
        }
        is ImeiCheck -> KeyValueRow(Res.string.imei_tac.str(), check.tac, copyable = false)
        is IsbnCheck -> check.other?.let { KeyValueRow(if (it.length == 13) "ISBN-13" else "ISBN-10", it) }
        is GtinCheck -> check.issuer?.let { issuer ->
            val name = when (issuer.use) {
                Gs1Use.COUNTRY -> issuer.countries.joinToString(", ") { regionName(it, lang) ?: it }
                Gs1Use.RESTRICTED -> Res.string.gs1_restricted.str()
                Gs1Use.OFFICE -> "GS1 Global Office"
                Gs1Use.ISSN -> Res.string.gs1_issn.str()
                Gs1Use.REFUND -> Res.string.gs1_refund.str()
                Gs1Use.COUPON -> Res.string.gs1_coupon.str()
            }
            KeyValueRow(Res.string.gs1_prefix.str(), name, mono = false, copyable = false)
            if (issuer.use == Gs1Use.COUNTRY) {
                Text(
                    Res.string.gs1_prefix_is_not_origin.str(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        is VinCheck -> {
            check.region?.let { KeyValueRow(Res.string.region.str(), vinRegionName(it), mono = false, copyable = false) }
            if (check.modelYears.isNotEmpty()) {
                KeyValueRow(
                    Res.string.model_year.str(),
                    check.modelYears.joinToString(" ${Res.string.or.str()} "),
                    mono = false,
                    copyable = false,
                )
            }
        }
    }
}

@Composable
private fun brandName(brand: CardBrand): String = when (brand) {
    CardBrand.VISA -> "Visa"
    CardBrand.MASTERCARD -> "Mastercard"
    CardBrand.MIR -> Res.string.card_mir.str()
    CardBrand.AMEX -> "American Express"
    CardBrand.UNIONPAY -> "UnionPay"
    CardBrand.JCB -> "JCB"
    CardBrand.DINERS -> "Diners Club"
    CardBrand.DISCOVER -> "Discover"
    CardBrand.MAESTRO -> "Maestro"
    CardBrand.UZCARD -> "Uzcard"
    CardBrand.HUMO -> "Humo"
    CardBrand.BELKART -> Res.string.card_belkart.str()
}

@Composable
private fun recordName(record: OgrnRecord): String = when (record) {
    OgrnRecord.OGRN -> Res.string.ogrn_record_main.str()
    OgrnRecord.GRN -> Res.string.ogrn_record_later.str()
    OgrnRecord.OGRNIP -> Res.string.ogrnip_record_main.str()
    OgrnRecord.GRNIP -> Res.string.ogrnip_record_later.str()
}

@Composable
private fun accountTypeName(type: AccountType): String = when (type) {
    AccountType.CORRESPONDENT -> Res.string.account_correspondent.str()
    AccountType.COMMERCIAL -> Res.string.account_commercial.str()
    AccountType.NON_PROFIT -> Res.string.account_non_profit.str()
    AccountType.ENTREPRENEUR -> Res.string.account_entrepreneur.str()
    AccountType.NON_RESIDENT_COMPANY -> Res.string.account_non_resident_company.str()
    AccountType.PERSON -> Res.string.account_person.str()
    AccountType.NON_RESIDENT_PERSON -> Res.string.account_non_resident_person.str()
    AccountType.DEPOSIT -> Res.string.account_deposit.str()
}

@Composable
private fun vinRegionName(region: VinRegion): String = when (region) {
    VinRegion.AFRICA -> Res.string.africa.str()
    VinRegion.ASIA -> Res.string.asia.str()
    VinRegion.EUROPE -> Res.string.europe.str()
    VinRegion.NORTH_AMERICA -> Res.string.north_america.str()
    VinRegion.OCEANIA -> Res.string.oceania.str()
    VinRegion.SOUTH_AMERICA -> Res.string.south_america.str()
}

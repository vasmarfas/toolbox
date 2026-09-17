package com.vasmarfas.card.tools.converters

import com.vasmarfas.card.core.Net
import com.vasmarfas.card.core.Prefs
import com.vasmarfas.card.resources.*
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject

class CurrencyRates(val rates: Map<String, Double>, val fetchedAt: Long)

object Currency {
    const val URL = "https://open.er-api.com/v6/latest/USD"
    const val MAX_AGE_MS = 12 * 60 * 60 * 1000L
    private const val RATES_KEY = "currency.rates"
    private const val TIME_KEY = "currency.rates.time"

    val popular = listOf("USD", "EUR", "GBP", "CNY", "JPY", "CHF", "RUB", "KZT", "TRY", "AED")

    val names = mapOf(
        "USD" to Res.string.us_dollar,
        "EUR" to Res.string.euro,
        "GBP" to Res.string.pound_sterling,
        "CNY" to Res.string.chinese_yuan,
        "JPY" to Res.string.japanese_yen,
        "CHF" to Res.string.swiss_franc,
        "RUB" to Res.string.russian_ruble,
        "KZT" to Res.string.kazakhstani_tenge,
        "TRY" to Res.string.turkish_lira,
        "AED" to Res.string.uae_dirham,
        "BYN" to Res.string.belarusian_ruble,
        "UAH" to Res.string.ukrainian_hryvnia,
        "GEL" to Res.string.georgian_lari,
        "AMD" to Res.string.armenian_dram,
        "UZS" to Res.string.uzbek_som,
        "KGS" to Res.string.kyrgyz_som,
        "INR" to Res.string.indian_rupee,
        "KRW" to Res.string.south_korean_won,
        "THB" to Res.string.thai_baht,
        "VND" to Res.string.vietnamese_dong,
        "CAD" to Res.string.canadian_dollar,
        "AUD" to Res.string.australian_dollar,
        "PLN" to Res.string.polish_zloty,
        "CZK" to Res.string.czech_koruna,
        "SEK" to Res.string.swedish_krona,
        "NOK" to Res.string.norwegian_krone,
        "ILS" to Res.string.israeli_shekel,
        "EGP" to Res.string.egyptian_pound,
        "BRL" to Res.string.brazilian_real,
        "MXN" to Res.string.mexican_peso,
        "HKD" to Res.string.hong_kong_dollar,
        "SGD" to Res.string.singapore_dollar,
        "RSD" to Res.string.serbian_dinar,
        "IDR" to Res.string.indonesian_rupiah,
    )

    fun parse(text: String): Map<String, Double>? {
        val rates = runCatching { Net.json.parseToJsonElement(text).jsonObject["rates"]?.jsonObject }.getOrNull() ?: return null
        val parsed = rates.mapNotNull { (code, value) -> (value as? JsonPrimitive)?.doubleOrNull?.let { code to it } }.toMap()
        return parsed.takeIf { it.containsKey("USD") }
    }

    fun convert(amount: Double, from: String, to: String, rates: Map<String, Double>): Double? {
        val fromRate = rates[from] ?: return null
        val toRate = rates[to] ?: return null
        return amount / fromRate * toRate
    }

    fun cached(): CurrencyRates? {
        val text = Prefs.store.get(RATES_KEY) ?: return null
        val time = Prefs.store.get(TIME_KEY)?.toLongOrNull() ?: return null
        return parse(text)?.let { CurrencyRates(it, time) }
    }

    fun store(text: String, time: Long) {
        Prefs.store.put(RATES_KEY, text)
        Prefs.store.put(TIME_KEY, time.toString())
    }
}

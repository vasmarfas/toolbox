package com.vasmarfas.card.tools.money

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vasmarfas.card.core.currentEpochMillis
import com.vasmarfas.card.core.fmt
import com.vasmarfas.card.core.str
import com.vasmarfas.card.core.toDoubleLenient
import com.vasmarfas.card.resources.*
import com.vasmarfas.card.tools.Tool
import com.vasmarfas.card.tools.ToolCategory
import com.vasmarfas.card.tools.converters.fmtSig
import com.vasmarfas.card.ui.components.ActionButton
import com.vasmarfas.card.ui.components.AnswerCard
import com.vasmarfas.card.ui.components.DropdownChoice
import com.vasmarfas.card.ui.components.ErrorText
import com.vasmarfas.card.ui.components.KeyValueRow
import com.vasmarfas.card.ui.components.LoadingRow
import com.vasmarfas.card.ui.components.NumberField
import com.vasmarfas.card.ui.components.ResultCard
import com.vasmarfas.card.ui.components.SegmentedChoice
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlinx.coroutines.CancellationException
import org.jetbrains.compose.resources.getString

val cryptoConverterTool = Tool(
    id = "crypto-converter",
    category = ToolCategory.MONEY,
    title = Res.string.crypto_converter,
    description = Res.string.crypto_converter_description,
    icon = Icons.Filled.CurrencyBitcoin,
    keywords = listOf(
        "crypto", "bitcoin", "btc", "ethereum", "eth", "usdt", "ton", "coin", "rate",
        "криптовалюта", "крипта", "биткоин", "эфир", "тон", "монета", "курс",
    ),
) { CryptoConverterScreen() }

@Composable
private fun CryptoConverterScreen() {
    var amountText by rememberSaveable { mutableStateOf("0.01") }
    var fromKind by rememberSaveable { mutableStateOf(AssetKind.CRYPTO) }
    var from by rememberSaveable { mutableStateOf("BTC") }
    var toKind by rememberSaveable { mutableStateOf(AssetKind.FIAT) }
    var to by rememberSaveable { mutableStateOf("USD") }
    var rates by remember { mutableStateOf(Currency.cached()) }
    var prices by remember { mutableStateOf(Crypto.cached()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableStateOf(0) }
    LaunchedEffect(refresh) {
        val now = currentEpochMillis()
        val fiatFresh = rates?.let { now - it.fetchedAt < Currency.MAX_AGE_MS } == true
        val cryptoFresh = prices?.let { now - it.fetchedAt < Crypto.MAX_AGE_MS } == true
        if (refresh == 0 && fiatFresh && cryptoFresh) return@LaunchedEffect
        loading = true
        error = null
        try {
            if (refresh > 0 || !fiatFresh) rates = Currency.fetch()
            if (refresh > 0 || !cryptoFresh) prices = Crypto.fetch()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = getString(Res.string.failed_to_load_rates) + ": " + (e.message ?: getString(Res.string.network_error))
        } finally {
            loading = false
        }
    }
    val fiatCodes = rates?.rates?.keys?.sorted() ?: listOf("EUR", "RUB", "USD")
    val coinCodes = Crypto.coins.map { it.code }
    fun codes(kind: AssetKind) = if (kind == AssetKind.FIAT) fiatCodes else coinCodes

    val amount = amountText.toDoubleLenient()
    NumberField(
        value = amountText,
        onValueChange = { amountText = it },
        label = Res.string.money_amount.str(),
        suffix = from,
        isError = amountText.isNotBlank() && amount == null,
    )
    AssetPicker(
        label = Res.string.from_.str(),
        kind = fromKind,
        code = from,
        codes = codes(fromKind),
        onKind = {
            fromKind = it
            from = if (it == AssetKind.FIAT) "USD" else "BTC"
        },
        onCode = { from = it },
    )
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        IconButton(
            onClick = {
                val kind = fromKind
                val code = from
                fromKind = toKind
                from = to
                toKind = kind
                to = code
            },
        ) {
            Icon(Icons.Filled.SwapVert, contentDescription = Res.string.swap.str())
        }
    }
    AssetPicker(
        label = Res.string.to.str(),
        kind = toKind,
        code = to,
        codes = codes(toKind),
        onKind = {
            toKind = it
            to = if (it == AssetKind.FIAT) "USD" else "ETH"
        },
        onCode = { to = it },
    )
    if (loading) LoadingRow(Res.string.loading_rates.str())
    error?.let { ErrorText(it) }
    val fiat = rates?.rates
    val usd = prices?.usd
    if (fiat != null && usd != null && amount != null) {
        fun convert(value: Double, a: AssetKind, x: String, b: AssetKind, y: String) = Crypto.convert(value, a, x, b, y, fiat, usd)
        convert(amount, fromKind, from, toKind, to)?.let { converted ->
            val rate = convert(1.0, fromKind, from, toKind, to)
            AnswerCard(
                "${assetAmount(converted, toKind)} $to",
                "${assetAmount(amount, fromKind)} $from" + (rate?.let { " · 1 $from = ${assetAmount(it, toKind)} $to" } ?: ""),
                copyValue = converted.fmtSig(10),
            )
        }
        ResultCard {
            convert(1.0, toKind, to, fromKind, from)?.let { KeyValueRow("1 $to", "${assetAmount(it, fromKind)} $from") }
            rates?.let { KeyValueRow(Res.string.rates_updated.str(), formatTime(it.fetchedAt), copyable = false) }
            prices?.let { KeyValueRow(Res.string.crypto_prices_updated.str(), formatTime(it.fetchedAt), copyable = false) }
        }
        val shownKind = if (fromKind == AssetKind.CRYPTO) AssetKind.FIAT else AssetKind.CRYPTO
        val shown = if (shownKind == AssetKind.FIAT) Currency.popular else Crypto.popular
        ResultCard(if (shownKind == AssetKind.FIAT) Res.string.popular_currencies.str() else Res.string.popular_coins.str()) {
            shown.filter { it != to }.forEach { code ->
                convert(amount, fromKind, from, shownKind, code)?.let { KeyValueRow(assetLabel(shownKind, code), "${assetAmount(it, shownKind)} $code") }
            }
        }
    }
    Text(Res.string.crypto_prices_source.str(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    ActionButton(
        text = Res.string.refresh_rates.str(),
        onClick = { refresh++ },
        enabled = !loading,
        icon = Icons.Filled.Refresh,
    )
}

@Composable
private fun AssetPicker(label: String, kind: AssetKind, code: String, codes: List<String>, onKind: (AssetKind) -> Unit, onCode: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        SegmentedChoice(
            options = AssetKind.entries,
            selected = kind,
            onSelect = onKind,
            label = { if (it == AssetKind.FIAT) Res.string.asset_fiat.str() else Res.string.asset_crypto.str() },
        )
        DropdownChoice(
            options = codes,
            selected = code,
            onSelect = onCode,
            label = if (kind == AssetKind.FIAT) Res.string.asset_fiat.str() else Res.string.asset_crypto.str(),
            text = { it },
            menuText = { assetLabel(kind, it) },
            searchable = true,
        )
    }
}

@Composable
private fun assetLabel(kind: AssetKind, code: String): String =
    if (kind == AssetKind.FIAT) currencyLabel(code) else Crypto.name(code)?.let { "$code — $it" } ?: code

// amounts below one keep about five significant digits: 0.00000014258 BTC, not 1.4258e-7
private fun assetAmount(value: Double, kind: AssetKind): String {
    val size = abs(value)
    return when {
        kind == AssetKind.FIAT -> money(value)
        size == 0.0 || size >= 1 -> value.fmt(if (size >= 1000) 2 else 6, grouping = true)
        else -> value.fmt((4 - floor(log10(size))).toInt().coerceAtMost(12))
    }
}

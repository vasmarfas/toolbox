package com.vasmarfas.card.tools.money

import com.vasmarfas.card.core.Net
import com.vasmarfas.card.core.Prefs
import com.vasmarfas.card.core.currentEpochMillis
import com.vasmarfas.card.resources.*
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import org.jetbrains.compose.resources.getString

class Coin(val code: String, val id: String, val name: String)

class CryptoPrices(val usd: Map<String, Double>, val fetchedAt: Long)

enum class AssetKind { FIAT, CRYPTO }

object Crypto {
    const val MAX_AGE_MS = 10 * 60 * 1000L
    private const val PRICES_KEY = "crypto.prices"
    private const val TIME_KEY = "crypto.prices.time"

    val coins = listOf(
        Coin("BTC", "bitcoin", "Bitcoin"), Coin("ETH", "ethereum", "Ethereum"), Coin("USDT", "tether", "Tether"),
        Coin("TON", "the-open-network", "Toncoin"), Coin("SOL", "solana", "Solana"), Coin("BNB", "binancecoin", "BNB"),
        Coin("XRP", "ripple", "XRP"), Coin("USDC", "usd-coin", "USD Coin"), Coin("DOGE", "dogecoin", "Dogecoin"),
        Coin("TRX", "tron", "TRON"), Coin("ADA", "cardano", "Cardano"), Coin("LTC", "litecoin", "Litecoin"),
        Coin("XMR", "monero", "Monero"), Coin("BCH", "bitcoin-cash", "Bitcoin Cash"), Coin("AVAX", "avalanche-2", "Avalanche"),
        Coin("DOT", "polkadot", "Polkadot"), Coin("LINK", "chainlink", "Chainlink"), Coin("XLM", "stellar", "Stellar"),
        Coin("ATOM", "cosmos", "Cosmos"), Coin("NEAR", "near", "NEAR"), Coin("SUI", "sui", "Sui"), Coin("APT", "aptos", "Aptos"),
        Coin("POL", "polygon-ecosystem-token", "Polygon"), Coin("ARB", "arbitrum", "Arbitrum"), Coin("OP", "optimism", "Optimism"),
        Coin("HBAR", "hedera-hashgraph", "Hedera"), Coin("ETC", "ethereum-classic", "Ethereum Classic"), Coin("UNI", "uniswap", "Uniswap"),
        Coin("DAI", "dai", "Dai"), Coin("SHIB", "shiba-inu", "Shiba Inu"), Coin("PEPE", "pepe", "Pepe"), Coin("NOT", "notcoin", "Notcoin"),
    )

    val popular = listOf("BTC", "ETH", "USDT", "TON", "SOL")

    private val url = "https://api.coingecko.com/api/v3/simple/price?ids=" + coins.joinToString(",") { it.id } + "&vs_currencies=usd"

    fun name(code: String): String? = coins.firstOrNull { it.code == code }?.name

    fun parse(text: String): Map<String, Double>? {
        val json = runCatching { Net.json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return null
        val prices = coins.mapNotNull { coin ->
            ((json[coin.id] as? JsonObject)?.get("usd") as? JsonPrimitive)?.doubleOrNull?.takeIf { it > 0 }?.let { coin.code to it }
        }.toMap()
        return prices.takeIf { it.containsKey("BTC") }
    }

    suspend fun fetch(): CryptoPrices {
        val text = Net.client.get(url).bodyAsText()
        val prices = parse(text) ?: throw IllegalStateException(getString(Res.string.unexpected_response))
        val now = currentEpochMillis()
        Prefs.store.put(PRICES_KEY, text)
        Prefs.store.put(TIME_KEY, now.toString())
        return CryptoPrices(prices, now)
    }

    fun cached(): CryptoPrices? {
        val text = Prefs.store.get(PRICES_KEY) ?: return null
        val time = Prefs.store.get(TIME_KEY)?.toLongOrNull() ?: return null
        return parse(text)?.let { CryptoPrices(it, time) }
    }

    // fiat rates are units per dollar, coin prices dollars per coin, everything goes through the dollar
    fun convert(amount: Double, fromKind: AssetKind, from: String, toKind: AssetKind, to: String, fiat: Map<String, Double>, usd: Map<String, Double>): Double? {
        fun dollarsPerUnit(kind: AssetKind, code: String): Double? = when (kind) {
            AssetKind.FIAT -> fiat[code]?.let { 1 / it }
            AssetKind.CRYPTO -> usd[code]
        }
        val source = dollarsPerUnit(fromKind, from) ?: return null
        val target = dollarsPerUnit(toKind, to)?.takeIf { it > 0 } ?: return null
        return amount * source / target
    }
}

package com.vasmarfas.card.tools.money

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CryptoRatesTest {
    private val fiat = mapOf("USD" to 1.0, "RUB" to 80.0, "EUR" to 0.9)
    private val usd = mapOf("BTC" to 80_000.0, "ETH" to 2_000.0, "USDT" to 1.0)

    @Test
    fun parsesCoinGeckoPrices() {
        val prices = assertNotNull(Crypto.parse("""{"bitcoin":{"usd":84207},"ethereum":{"usd":2681.28},"the-open-network":{"usd":1.42}}"""))
        assertEquals(84207.0, prices["BTC"])
        assertEquals(2681.28, prices["ETH"])
        assertEquals(1.42, prices["TON"])
        assertNull(Crypto.parse("""{"ethereum":{"usd":2681.28}}"""))
        assertNull(Crypto.parse("not json"))
    }

    @Test
    fun convertsBetweenMoneyAndCoinsThroughTheDollar() {
        assertEquals(800.0, Crypto.convert(0.01, AssetKind.CRYPTO, "BTC", AssetKind.FIAT, "USD", fiat, usd)!!, 1e-9)
        assertEquals(64_000.0, Crypto.convert(0.01, AssetKind.CRYPTO, "BTC", AssetKind.FIAT, "RUB", fiat, usd)!!, 1e-9)
        assertEquals(0.5, Crypto.convert(80_000.0, AssetKind.FIAT, "RUB", AssetKind.CRYPTO, "ETH", fiat, usd)!!, 1e-9)
        assertEquals(40.0, Crypto.convert(1.0, AssetKind.CRYPTO, "BTC", AssetKind.CRYPTO, "ETH", fiat, usd)!!, 1e-9)
        assertEquals(90.0, Crypto.convert(100.0, AssetKind.FIAT, "USD", AssetKind.FIAT, "EUR", fiat, usd)!!, 1e-9)
        assertNull(Crypto.convert(1.0, AssetKind.CRYPTO, "DOGE", AssetKind.FIAT, "USD", fiat, usd))
    }
}

package com.vasmarfas.card.tools.everyday

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IdentifiersTest {
    private fun check(number: String, bik: String = "") = Identifiers.check(number, bik, 2026)

    private inline fun <reified T : IdCheck> only(number: String, bik: String = ""): T =
        check(number, bik).filterIsInstance<T>().single()

    @Test
    fun cards() {
        val visa = only<CardCheck>("4111 1111 1111 1111")
        assertTrue(visa.valid)
        assertEquals(CardBrand.VISA, visa.brand)
        assertEquals("4111 1111 1111 1111", visa.formatted)
        assertEquals(CardBrand.MIR, only<CardCheck>("2200000000000004").brand)
        assertEquals(CardBrand.MASTERCARD, only<CardCheck>("5555555555554444").brand)
        val amex = only<CardCheck>("378282246310005")
        assertTrue(amex.valid)
        assertEquals("3782 822463 10005", amex.formatted)
        val typo = only<CardCheck>("4111 1111 1111 1112")
        assertFalse(typo.valid)
        assertEquals("1", typo.expected)
        val long = only<CardCheck>("5555 5555 5555 4444 8")
        assertFalse(long.lengthFits)
    }

    @Test
    fun iban() {
        val de = only<IbanCheck>("DE89 3704 0044 0532 0130 00")
        assertTrue(de.valid)
        assertEquals("DE", de.country)
        assertTrue(only<IbanCheck>("gb82 west 1234 5698 7654 32").valid)
        val wrong = only<IbanCheck>("DE88370400440532013000")
        assertFalse(wrong.valid)
        assertEquals("89", wrong.expected)
        val short = only<IbanCheck>("DE8937040044053201300")
        assertEquals(22, short.countryLength)
        assertFalse(short.valid)
        assertNull(only<IbanCheck>("US64SVBKUS6S3300958879").countryLength)
    }

    @Test
    fun russianRegistryNumbers() {
        val company = only<InnCheck>("7707083893")
        assertTrue(company.valid)
        assertTrue(company.organization)
        assertEquals("7707", company.taxOffice)
        val person = only<InnCheck>("500100732259")
        assertTrue(person.valid)
        assertFalse(person.organization)
        assertEquals("59", only<InnCheck>("500100732250").expected)
        assertFalse(only<InnCheck>("0000000000").valid)

        val snils = only<SnilsCheck>("112-233-445 95")
        assertTrue(snils.valid)
        assertEquals("112-233-445 95", snils.formatted)
        assertEquals("95", only<SnilsCheck>("11223344594").expected)
        assertFalse(only<SnilsCheck>("00100199800").checked)

        val ogrn = only<OgrnCheck>("1027700132195")
        assertTrue(ogrn.valid)
        assertEquals(OgrnRecord.OGRN, ogrn.record)
        assertEquals(2002, ogrn.year)
        assertEquals("77", ogrn.region)
        val ogrnip = only<OgrnCheck>("304500116000157")
        assertTrue(ogrnip.valid)
        assertEquals(OgrnRecord.OGRNIP, ogrnip.record)
        assertEquals(2004, ogrnip.year)
        assertFalse(only<OgrnCheck>("1997700132190").valid)
    }

    @Test
    fun bankAccount() {
        val correspondent = only<AccountCheck>("30101810400000000225", "044525225")
        assertTrue(correspondent.valid)
        assertEquals(AccountType.CORRESPONDENT, correspondent.type)
        assertEquals("30101 810 4 0000 0000225", correspondent.formatted)
        val wrong = only<AccountCheck>("30101810500000000225", "044525225")
        assertEquals("4", wrong.expected)
        val missing = only<AccountCheck>("40702810400000012345")
        assertFalse(missing.bikFits)
        assertEquals("810", missing.currency)
        assertEquals(AccountType.COMMERCIAL, missing.type)
    }

    @Test
    fun devicesBooksAndGoods() {
        val imei = only<ImeiCheck>("49-015420-323751-8")
        assertTrue(imei.valid)
        assertEquals("49015420", imei.tac)

        val isbn13 = only<IsbnCheck>("978-0-306-40615-7")
        assertTrue(isbn13.valid)
        assertEquals("0306406152", isbn13.other)
        assertEquals("9780306406157", only<IsbnCheck>("0-306-40615-2").other)
        assertTrue(only<IsbnCheck>("0-8044-2957-x").valid)
        assertEquals("X", only<IsbnCheck>("0804429570").expected)
        assertTrue(check("978-0-306-40615-7").none { it is GtinCheck })

        val ean = only<GtinCheck>("4601234567893")
        assertTrue(ean.valid)
        assertEquals(listOf("RU"), ean.issuer?.countries)
        assertEquals(listOf("US"), only<GtinCheck>("036000291452").issuer?.countries)
        assertEquals(Gs1Use.RESTRICTED, only<GtinCheck>("20123451").issuer?.use)
        assertEquals(Gs1Use.ISSN, only<GtinCheck>("9771234567003").issuer?.use)
    }

    @Test
    fun vin() {
        val vin = only<VinCheck>("1M8GDM9AXKP042788")
        assertTrue(vin.valid)
        assertEquals(VinRegion.NORTH_AMERICA, vin.region)
        assertEquals(listOf(1989, 2019), vin.modelYears)
        val american = only<VinCheck>("1M8GDM9A1KP042788")
        assertFalse(american.valid)
        assertEquals("X", american.expected)
        val european = only<VinCheck>("WVWZZZ1KZ6W000001")
        assertTrue(european.valid)
        assertEquals(VinRegion.EUROPE, european.region)
    }

    @Test
    fun detection() {
        assertTrue(check("").isEmpty())
        assertTrue(check("12345").isEmpty())
        val thirteen = check("4601234567893")
        assertEquals(2, thirteen.size)
        assertIs<GtinCheck>(thirteen.first())
        assertIs<OgrnCheck>(thirteen.last())
        assertIs<CardCheck>(check("378282246310006").first { it is CardCheck })
        assertEquals(listOf(true, false), check("1027700132195").map { it is OgrnCheck })
    }
}

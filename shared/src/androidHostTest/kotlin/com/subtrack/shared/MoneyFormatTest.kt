package com.subtrack.shared

import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MoneyFormatTest {
    private lateinit var originalLocale: Locale

    @BeforeTest
    fun setUp() {
        originalLocale = Locale.getDefault()
    }

    @AfterTest
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    /** Espaço não-quebrável aparece entre símbolo e número em vários locales. */
    private fun String.normalizeSpaces() = replace(' ', ' ').replace(' ', ' ')

    @Test
    fun formatsUsingBrazilianConventions() {
        Locale.setDefault(Locale.forLanguageTag("pt-BR"))
        assertEquals("R$ 54,90", formatMoney(5490, "BRL").normalizeSpaces())
    }

    @Test
    fun formatsUsingUnitedStatesConventions() {
        Locale.setDefault(Locale.US)
        assertEquals("$54.90", formatMoney(5490, "USD").normalizeSpaces())
    }

    @Test
    fun theSameAmountFollowsWhicheverLocaleIsActive() {
        Locale.setDefault(Locale.forLanguageTag("pt-BR"))
        val brazilian = formatMoney(123456, "BRL").normalizeSpaces()
        Locale.setDefault(Locale.US)
        val american = formatMoney(123456, "USD").normalizeSpaces()

        assertEquals("R$ 1.234,56", brazilian)
        assertEquals("$1,234.56", american)
    }

    @Test
    fun keepsTrailingZeroCents() {
        Locale.setDefault(Locale.US)
        assertEquals("$10.00", formatMoney(1000, "USD").normalizeSpaces())
        assertEquals("$10.50", formatMoney(1050, "USD").normalizeSpaces())
    }

    @Test
    fun handlesZero() {
        Locale.setDefault(Locale.US)
        assertEquals("$0.00", formatMoney(0, "USD").normalizeSpaces())
    }

    @Test
    fun largeAmountsDoNotLoseCents() {
        // Passar centavos por Double perderia precisão bem antes disto; a
        // conversão é por BigDecimal com escala fixa justamente por isso.
        Locale.setDefault(Locale.US)
        val cents = 92_233_720_368_547L
        val formatted = formatMoney(cents, "USD").normalizeSpaces()
        assertTrue(formatted.endsWith(".47"), "centavos perdidos: $formatted")
    }

    @Test
    fun unknownCurrencyCodeFailsLoudly() {
        Locale.setDefault(Locale.US)
        val thrown = runCatching { formatMoney(100, "XYZ_NOT_A_CURRENCY") }.exceptionOrNull()
        assertTrue(thrown is IllegalArgumentException, "esperava IllegalArgumentException, veio $thrown")
    }
}

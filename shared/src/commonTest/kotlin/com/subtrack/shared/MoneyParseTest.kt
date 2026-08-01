package com.subtrack.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MoneyParseTest {
    @Test
    fun acceptsCommaAsDecimalSeparator() {
        assertEquals(5490, parseMoneyToCents("54,90"))
    }

    @Test
    fun acceptsDotAsDecimalSeparator() {
        assertEquals(5490, parseMoneyToCents("54.90"))
    }

    @Test
    fun wholeNumbersBecomeWholeCents() {
        assertEquals(10000, parseMoneyToCents("100"))
        assertEquals(0, parseMoneyToCents("0"))
    }

    @Test
    fun threeDigitsAfterASeparatorCanOnlyBeThousands() {
        // Decimal tem sempre 2 casas, então 3 dígitos não têm outra leitura.
        assertEquals(123400, parseMoneyToCents("1.234"))
        assertEquals(123400, parseMoneyToCents("1,234"))
        assertEquals(123456700, parseMoneyToCents("1.234.567"))
        assertEquals(5490100, parseMoneyToCents("54,901"))
    }

    @Test
    fun mixedSeparatorsTreatTheLastOneAsDecimal() {
        // Ambas as convenções chegam no mesmo valor.
        assertEquals(123456, parseMoneyToCents("1.234,56"))
        assertEquals(123456, parseMoneyToCents("1,234.56"))
    }

    @Test
    fun ignoresCurrencySymbolsAndSpaces() {
        assertEquals(5490, parseMoneyToCents("R$ 54,90"))
        assertEquals(5490, parseMoneyToCents("  $54.90 "))
    }

    @Test
    fun rejectsADecimalPartThatIsNotExactlyTwoDigits() {
        // Uma casa é entrada pela metade, quatro não é nem decimal nem milhar.
        assertNull(parseMoneyToCents("54,9"))
        assertNull(parseMoneyToCents("54,9012"))
        assertNull(parseMoneyToCents("1.234,567"))
    }

    @Test
    fun rejectsEmptyAndNonNumericInput() {
        assertNull(parseMoneyToCents(""))
        assertNull(parseMoneyToCents("   "))
        assertNull(parseMoneyToCents("abc"))
        assertNull(parseMoneyToCents(","))
    }

    @Test
    fun rejectsNegativeAmounts() {
        // Assinatura com valor negativo não existe; barrar aqui evita um
        // require() estourando lá no RecurringCost.
        assertNull(parseMoneyToCents("-10"))
    }

    @Test
    fun leadingSeparatorIsReadAsZeroUnits() {
        assertEquals(90, parseMoneyToCents(",90"))
        assertEquals(90, parseMoneyToCents(".90"))
    }

    @Test
    fun parsingSurvivesARoundTripThroughFormatting() {
        listOf(0L, 1L, 99L, 100L, 5490L, 123456L).forEach { cents ->
            assertEquals(cents, parseMoneyToCents(centsAsPlainText(cents)), "falhou em $cents")
        }
    }

    private fun centsAsPlainText(cents: Long): String = "${cents / 100},${(cents % 100).toString().padStart(2, '0')}"
}

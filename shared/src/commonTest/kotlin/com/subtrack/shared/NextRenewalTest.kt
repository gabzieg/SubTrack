package com.subtrack.shared

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NextRenewalTest {
    private fun date(iso: String) = LocalDate.parse(iso)

    @Test
    fun anchorInTheFutureIsItsOwnAnswer() {
        assertEquals(
            date("2026-09-10"),
            nextRenewalOnOrAfter(date("2026-09-10"), BillingCycle.MONTHLY, 1, from = date("2026-07-30")),
        )
    }

    @Test
    fun renewalDueTodayCountsAsNext() {
        assertEquals(
            date("2026-07-30"),
            nextRenewalOnOrAfter(date("2026-01-30"), BillingCycle.MONTHLY, 1, from = date("2026-07-30")),
        )
    }

    @Test
    fun monthlyDoesNotDriftAfterAShortMonth() {
        // O ponto central: cobrança dia 31 cai em 28/fev, mas março volta pro 31.
        // Se avançássemos a partir da renovação anterior, março viraria 28.
        val anchor = date("2026-01-31")
        assertEquals(
            date("2026-02-28"),
            nextRenewalOnOrAfter(anchor, BillingCycle.MONTHLY, 1, from = date("2026-02-01")),
        )
        assertEquals(
            date("2026-03-31"),
            nextRenewalOnOrAfter(anchor, BillingCycle.MONTHLY, 1, from = date("2026-03-01")),
        )
        assertEquals(
            date("2026-04-30"),
            nextRenewalOnOrAfter(anchor, BillingCycle.MONTHLY, 1, from = date("2026-04-01")),
        )
    }

    @Test
    fun monthlySkipsAcrossYearBoundary() {
        assertEquals(
            date("2027-01-15"),
            nextRenewalOnOrAfter(date("2025-03-15"), BillingCycle.MONTHLY, 1, from = date("2026-12-16")),
        )
    }

    @Test
    fun everyNMonthsLandsOnlyOnValidOccurrences() {
        val anchor = date("2026-01-15")
        // Ciclo trimestral: 15/jan, 15/abr, 15/jul, 15/out.
        assertEquals(
            date("2026-04-15"),
            nextRenewalOnOrAfter(anchor, BillingCycle.MONTHLY, 3, from = date("2026-02-01")),
        )
        assertEquals(
            date("2026-07-15"),
            nextRenewalOnOrAfter(anchor, BillingCycle.MONTHLY, 3, from = date("2026-04-16")),
        )
    }

    @Test
    fun yearlyHandlesLeapDayAnchor() {
        // 29/fev só existe em ano bissexto — nos demais trunca pro dia 28,
        // e volta pro 29 quando o ano bissexto chega de novo.
        val anchor = date("2024-02-29")
        assertEquals(
            date("2027-02-28"),
            nextRenewalOnOrAfter(anchor, BillingCycle.YEARLY, 1, from = date("2026-03-01")),
        )
        assertEquals(
            date("2028-02-29"),
            nextRenewalOnOrAfter(anchor, BillingCycle.YEARLY, 1, from = date("2027-03-01")),
        )
    }

    @Test
    fun weeklyStaysOnTheSameWeekday() {
        val anchor = date("2026-01-01") // quinta-feira
        val next = nextRenewalOnOrAfter(anchor, BillingCycle.WEEKLY, 1, from = date("2026-07-30"))
        assertEquals(anchor.dayOfWeek, next.dayOfWeek)
        assertTrue(next >= date("2026-07-30"))
    }

    @Test
    fun dailyEveryNKeepsTheStride() {
        val anchor = date("2026-07-01")
        assertEquals(
            date("2026-07-31"),
            nextRenewalOnOrAfter(anchor, BillingCycle.DAILY, 10, from = date("2026-07-25")),
        )
    }

    @Test
    fun resultIsAlwaysOnOrAfterTheReferenceDate() {
        val anchor = date("2026-01-31")
        val from = date("2026-07-30")
        BillingCycle.entries.forEach { cycle ->
            val next = nextRenewalOnOrAfter(anchor, cycle, everyN = 1, from = from)
            assertTrue(next >= from, "$cycle devolveu $next, anterior a $from")
        }
    }

    @Test
    fun rejectsInvalidEveryN() {
        assertFailsWith<IllegalArgumentException> {
            nextRenewalOnOrAfter(date("2026-01-01"), BillingCycle.MONTHLY, everyN = 0, from = date("2026-07-30"))
        }
    }
}

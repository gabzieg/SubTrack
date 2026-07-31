package com.subtrack.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RecurringCostTest {
    @Test
    fun monthlySubscriptionKeepsItsAmountPerMonth() {
        val netflix = RecurringCost(amountCents = 5490, cycle = BillingCycle.MONTHLY)
        assertEquals(5490, netflix.monthlyCents())
        assertEquals(65880, netflix.yearlyCents())
    }

    @Test
    fun yearlySubscriptionSpreadsOverTwelveMonths() {
        val annualPlan = RecurringCost(amountCents = 12000, cycle = BillingCycle.YEARLY)
        assertEquals(1000, annualPlan.monthlyCents())
        assertEquals(12000, annualPlan.yearlyCents())
    }

    @Test
    fun weeklySubscriptionUsesCalendarWeeksNotFourPerMonth() {
        // 52,18 semanas por ano — não 48. O ciclo de 4 semanas que os concorrentes
        // usam pra cobrar 13 vezes por ano é exatamente essa diferença (ver SPEC.md).
        val weekly = RecurringCost(amountCents = 1000, cycle = BillingCycle.WEEKLY)
        assertEquals(52179, weekly.yearlyCents())
        assertEquals(4348, weekly.monthlyCents())
    }

    @Test
    fun everyNDividesTheFrequency() {
        val everyTwoMonths = RecurringCost(amountCents = 6000, cycle = BillingCycle.MONTHLY, everyN = 2)
        assertEquals(3000, everyTwoMonths.monthlyCents())
        assertEquals(36000, everyTwoMonths.yearlyCents())
    }

    @Test
    fun walletTotalAnnualizesBeforeSumming() {
        val wallet = listOf(
            RecurringCost(amountCents = 5490, cycle = BillingCycle.MONTHLY),
            RecurringCost(amountCents = 12000, cycle = BillingCycle.YEARLY),
        )
        assertEquals(77880, wallet.totalYearlyCents())
        assertEquals(6490, wallet.totalMonthlyCents())
    }

    @Test
    fun rejectsInvalidEveryN() {
        assertFailsWith<IllegalArgumentException> {
            RecurringCost(amountCents = 100, cycle = BillingCycle.MONTHLY, everyN = 0)
        }
    }

    @Test
    fun cycleRoundTripsThroughStorage() {
        BillingCycle.entries.forEach { cycle ->
            assertEquals(cycle, BillingCycle.fromStorage(cycle.name))
        }
    }
}

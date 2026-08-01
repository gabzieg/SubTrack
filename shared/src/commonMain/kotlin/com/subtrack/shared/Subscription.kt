package com.subtrack.shared

import kotlinx.datetime.LocalDate

/**
 * Uma assinatura já persistida. [anchor] é a primeira cobrança, não a próxima —
 * a próxima é sempre derivada (ver NextRenewal.kt).
 */
data class Subscription(
    val id: Long,
    val name: String,
    val amountCents: Long,
    val currency: String,
    val cycle: BillingCycle,
    val everyN: Int,
    val anchor: LocalDate,
    val category: String?,
    val notifyDaysBefore: Int,
    val archived: Boolean,
)

/** Assinatura ainda sem id, na forma que a tela de cadastro produz. */
data class SubscriptionDraft(
    val name: String,
    val amountCents: Long,
    val currency: String,
    val cycle: BillingCycle,
    val everyN: Int = 1,
    val anchor: LocalDate,
    val category: String? = null,
    val notifyDaysBefore: Int = 3,
)

/** Assinatura junto da renovação calculada pra uma data de referência. */
data class UpcomingSubscription(
    val subscription: Subscription,
    val nextRenewal: LocalDate,
)

fun Subscription.toRecurringCost(): RecurringCost = RecurringCost(amountCents = amountCents, cycle = cycle, everyN = everyN)

fun Subscription.nextRenewalOnOrAfter(from: LocalDate): LocalDate = nextRenewalOnOrAfter(anchor = anchor, cycle = cycle, everyN = everyN, from = from)

package com.subtrack.shared

import kotlin.math.roundToLong

/**
 * Valor sempre em centavos (Long) — ponto flutuante não entra em dinheiro
 * persistido, só no cálculo intermediário de anualização.
 */
data class RecurringCost(
    val amountCents: Long,
    val cycle: BillingCycle,
    val everyN: Int = 1,
) {
    init {
        require(everyN >= 1) { "everyN precisa ser >= 1, recebido $everyN" }
        require(amountCents >= 0) { "amountCents não pode ser negativo, recebido $amountCents" }
    }
}

private const val DAYS_PER_YEAR = 365.25
private const val DAYS_PER_WEEK = 7.0
private const val MONTHS_PER_YEAR = 12.0

private fun RecurringCost.occurrencesPerYear(): Double {
    val base = when (cycle) {
        BillingCycle.DAILY -> DAYS_PER_YEAR
        BillingCycle.WEEKLY -> DAYS_PER_YEAR / DAYS_PER_WEEK
        BillingCycle.MONTHLY -> MONTHS_PER_YEAR
        BillingCycle.YEARLY -> 1.0
    }
    return base / everyN
}

fun RecurringCost.yearlyCents(): Long = (amountCents * occurrencesPerYear()).roundToLong()

fun RecurringCost.monthlyCents(): Long = (amountCents * occurrencesPerYear() / MONTHS_PER_YEAR).roundToLong()

/**
 * Soma anualizada de uma carteira inteira. Anualiza cada item antes de somar —
 * somar mensal arredondado item a item acumularia erro de arredondamento.
 */
fun Iterable<RecurringCost>.totalYearlyCents(): Long = sumOf { it.yearlyCents() }

fun Iterable<RecurringCost>.totalMonthlyCents(): Long = (sumOf { it.amountCents * it.occurrencesPerYear() } / MONTHS_PER_YEAR).roundToLong()

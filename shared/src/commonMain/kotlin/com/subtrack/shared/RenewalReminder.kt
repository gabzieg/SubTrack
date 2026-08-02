package com.subtrack.shared

import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

/**
 * Um aviso a disparar: qual assinatura, em que dia, e quantos dias antes da
 * cobrança o usuário pediu pra ser avisado.
 */
data class RenewalReminder(
    val subscriptionId: Long,
    val subscriptionName: String,
    val amountCents: Long,
    val currency: String,
    val renewalDate: LocalDate,
    val remindOn: LocalDate,
) {
    val daysBeforeRenewal: Int get() = remindOn.daysUntil(renewalDate)
}

/**
 * Decide quais avisos existem a partir de [from].
 *
 * Separado do agendamento de propósito: isto é função pura sobre datas, então
 * dá pra testar cada caso de borda sem emulador nem relógio. O WorkManager só
 * recebe o resultado.
 *
 * Um aviso cuja data já passou (app ficou semanas fechado, aparelho desligado)
 * não é descartado — é puxado pra [from]. Perder o aviso em silêncio seria a
 * falha mais cara possível pra este app, que existe justamente pra avisar.
 */
fun List<UpcomingSubscription>.remindersOnOrAfter(from: LocalDate): List<RenewalReminder> = map { upcoming ->
    val subscription = upcoming.subscription
    val naturalRemindOn = upcoming.nextRenewal.minusDays(subscription.notifyDaysBefore)
    RenewalReminder(
        subscriptionId = subscription.id,
        subscriptionName = subscription.name,
        amountCents = subscription.amountCents,
        currency = subscription.currency,
        renewalDate = upcoming.nextRenewal,
        remindOn = if (naturalRemindOn < from) from else naturalRemindOn,
    )
}.sortedWith(compareBy({ it.remindOn }, { it.subscriptionName }))

private fun LocalDate.minusDays(days: Int): LocalDate = LocalDate.fromEpochDays(toEpochDays() - days)

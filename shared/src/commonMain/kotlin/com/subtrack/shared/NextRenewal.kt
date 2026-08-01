package com.subtrack.shared

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.monthsUntil
import kotlinx.datetime.plus

/**
 * Calcula a próxima renovação sempre a partir da data-âncora, nunca a partir da
 * renovação anterior já calculada. Cobrança dia 31 cai em 28/fev, e março volta
 * pro dia 31 — o dia não derrete mês a mês.
 *
 * ATENÇÃO: o mercado tem DOIS comportamentos e este é só um deles.
 *
 *  - Stripe e Recurly truncam e voltam: 31/jan, 28/fev, 31/mar. É o que está
 *    implementado aqui, e vale pra cobrança direto no cartão.
 *  - Google Play trava no dia truncado PARA SEMPRE. A doc oficial é explícita:
 *    "moved to the 28th of February and continues to renew on the 28th of each
 *    month for the duration of the subscription"
 *    (developer.android.com/google/play/billing/lifecycle/subscriptions).
 *    A App Store aparenta fazer o mesmo — evidência de segunda mão, não da doc
 *    da Apple.
 *
 * O que segura a diferença na prática é a âncora ser recente: qualquer cobrança
 * que de fato ocorreu serve de âncora, porque o cálculo anda em ciclos inteiros.
 * Se o usuário informa a cobrança que está na fatura, uma eventual derivação da
 * Play Store já veio embutida nessa data, e as duas séries coincidem daí pra
 * frente. A divergência só reaparece se a âncora for antiga — de antes da
 * derivação ter acontecido.
 *
 * @param anchor uma data de cobrança real da assinatura, de preferência recente.
 * @param from data de referência (normalmente hoje).
 * @return a primeira ocorrência em [from] ou depois. Se [anchor] ainda está no
 *   futuro, a própria âncora é a resposta.
 */
fun nextRenewalOnOrAfter(
    anchor: LocalDate,
    cycle: BillingCycle,
    everyN: Int,
    from: LocalDate,
): LocalDate {
    require(everyN >= 1) { "everyN precisa ser >= 1, recebido $everyN" }

    if (anchor >= from) return anchor

    return when (cycle) {
        BillingCycle.DAILY -> anchor.advanceByDays(stepDays = everyN, from = from)
        BillingCycle.WEEKLY -> anchor.advanceByDays(stepDays = everyN * 7, from = from)
        BillingCycle.MONTHLY -> anchor.advanceByMonths(stepMonths = everyN, from = from)
        BillingCycle.YEARLY -> anchor.advanceByMonths(stepMonths = everyN * 12, from = from)
    }
}

private fun LocalDate.advanceByDays(
    stepDays: Int,
    from: LocalDate,
): LocalDate {
    val elapsed = this.daysUntil(from)
    // Divisão inteira com arredondamento pra cima: queremos a primeira
    // ocorrência que alcança `from`, não a última que ficou pra trás.
    val steps = (elapsed + stepDays - 1) / stepDays
    return this.plus(steps.toLong() * stepDays, DateTimeUnit.DAY)
}

private fun LocalDate.advanceByMonths(
    stepMonths: Int,
    from: LocalDate,
): LocalDate {
    var steps = this.monthsUntil(from) / stepMonths

    // O palpite acima ignora o dia do mês, então pode cair um passo antes ou
    // depois. Corrige nos dois sentidos — no máximo uma iteração de cada lado.
    while (occurrence(steps, stepMonths) < from) steps++
    while (steps > 0 && occurrence(steps - 1, stepMonths) >= from) steps--

    return occurrence(steps, stepMonths)
}

/**
 * `plus` com unidade de mês já trunca pro último dia válido (31/jan + 1 mês =
 * 28/fev). Como partimos sempre da âncora, esse truncamento não acumula.
 */
private fun LocalDate.occurrence(
    steps: Int,
    stepMonths: Int,
): LocalDate = this.plus(steps.toLong() * stepMonths, DateTimeUnit.MONTH)

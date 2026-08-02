package com.subtrack.shared

import kotlinx.datetime.LocalDate

/**
 * Colunas na ordem exportada. `next_renewal` é derivada — está aqui pra quem
 * abrir o arquivo numa planilha, e um import futuro deve ignorá-la (a fonte da
 * verdade é `charge_date`).
 */
private val HEADER = listOf(
    "name",
    "amount",
    "currency",
    "cycle",
    "every_n",
    "charge_date",
    "category",
    "notify_days_before",
    "archived",
    "next_renewal",
)

/**
 * Gera o CSV da carteira inteira, arquivadas incluídas — é backup, e um backup
 * que descarta o histórico não é backup.
 *
 * Segue RFC 4180: separador vírgula e decimal com ponto. Numa planilha
 * configurada em pt-BR o Excel pode pedir pra escolher o separador na
 * importação; o preço de usar o padrão interoperável em vez de um formato que
 * só abre bem num locale.
 */
fun List<UpcomingSubscription>.toCsv(): String {
    val rows = map { upcoming ->
        val subscription = upcoming.subscription
        listOf(
            subscription.name,
            centsToPlainDecimal(subscription.amountCents),
            subscription.currency,
            subscription.cycle.name,
            subscription.everyN.toString(),
            subscription.anchor.toString(),
            subscription.category.orEmpty(),
            subscription.notifyDaysBefore.toString(),
            if (subscription.archived) "true" else "false",
            upcoming.nextRenewal.toString(),
        )
    }

    // \r\n é o que a RFC 4180 manda e o que o Excel espera.
    return (listOf(HEADER) + rows).joinToString(separator = "\r\n") { row ->
        row.joinToString(separator = ",") { escapeCsvField(it) }
    } + "\r\n"
}

/**
 * Sempre 2 casas e sem separador de milhar: "1234.50", não "1,234.50" — o
 * separador de milhar colidiria com o separador de campo.
 */
internal fun centsToPlainDecimal(cents: Long): String {
    val units = cents / 100
    val remainder = (cents % 100).toString().padStart(2, '0')
    return "$units.$remainder"
}

/**
 * Campo só é envolvido em aspas quando precisa. Sem isso, um nome com vírgula
 * ("Spotify, plano família") viraria duas colunas e desalinharia a linha
 * inteira, silenciosamente.
 */
internal fun escapeCsvField(value: String): String {
    val needsQuoting = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
    if (!needsQuoting) return value
    return "\"" + value.replace("\"", "\"\"") + "\""
}

/** Nome de arquivo estável e ordenável, sem caractere que o SO recuse. */
fun csvFileName(today: LocalDate): String = "subtrack-$today.csv"

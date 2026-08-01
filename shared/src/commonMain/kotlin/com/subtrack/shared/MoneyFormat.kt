package com.subtrack.shared

/**
 * Formata centavos usando as convenções da plataforma (símbolo, separador
 * decimal, posição do símbolo). Delegar pro SO é o que faz "R$ 54,90" e
 * "$54.90" saírem certos sem manter tabela de locale na mão.
 *
 * O valor entra em centavos e é convertido com escala fixa de 2 casas — nunca
 * passa por ponto flutuante, que arredondaria centavo em valores grandes.
 */
expect fun formatMoney(
    amountCents: Long,
    currencyCode: String,
): String

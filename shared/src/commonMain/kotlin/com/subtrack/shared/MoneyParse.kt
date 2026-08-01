package com.subtrack.shared

/**
 * Converte texto monetário em centavos. Valor sempre tem exatamente 2 casas
 * decimais, e é isso que dissolve a ambiguidade de "1.234": como decimal nunca
 * tem 3 dígitos, um separador seguido de 3 dígitos só pode ser agrupamento de
 * milhar. Vírgula e ponto são aceitos indistintamente.
 *
 * O formulário do app nem chega aqui — lá a entrada é só de dígitos e os
 * centavos são preenchidos da direita pra esquerda. Esta função existe pro
 * caminho de import (Fase 3), onde o texto vem de fora e precisa ser validado.
 *
 * @return centavos, ou null se a entrada não for um valor monetário válido.
 */
fun parseMoneyToCents(input: String): Long? {
    if (input.contains('-')) return null

    val cleaned = input.filter { it.isDigit() || it == '.' || it == ',' }
    if (cleaned.isEmpty() || cleaned.none { it.isDigit() }) return null

    val lastSeparator = cleaned.indexOfLast { it == '.' || it == ',' }
    val digitsAfterSeparator = if (lastSeparator < 0) 0 else cleaned.length - lastSeparator - 1
    val mixesBothSeparatorSymbols = cleaned.any { it == '.' } && cleaned.any { it == ',' }

    val separatorIsDecimal =
        when {
            lastSeparator < 0 -> false
            // Símbolos misturados ("1.234,56") só fazem sentido com o último
            // sendo o decimal — então ele precisa ter as 2 casas, ou a entrada
            // está malformada.
            mixesBothSeparatorSymbols -> if (digitsAfterSeparator == 2) true else return null
            // Exatamente 2 dígitos: é o decimal.
            digitsAfterSeparator == 2 -> true
            // Exatamente 3: só pode ser milhar, já que decimal tem sempre 2.
            digitsAfterSeparator == 3 -> false
            // Qualquer outra quantidade não é nem uma coisa nem outra.
            else -> return null
        }

    val integerText = if (separatorIsDecimal) cleaned.substring(0, lastSeparator) else cleaned
    val decimalText = if (separatorIsDecimal) cleaned.substring(lastSeparator + 1) else "00"

    // O que sobrou de separador no lado inteiro só pode ser agrupamento.
    val units = integerText.filter { it.isDigit() }.ifEmpty { "0" }.toLongOrNull() ?: return null
    val cents = decimalText.toLongOrNull() ?: return null

    return units * 100 + cents
}

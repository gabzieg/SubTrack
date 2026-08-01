package com.subtrack.shared

/**
 * Converte o que o usuário digitou em centavos, aceitando vírgula ou ponto como
 * separador decimal sem exigir que ele saiba qual o app espera.
 *
 * A ambiguidade real é "1.234": pode ser mil duzentos e trinta e quatro ou um
 * e vinte e três. As regras abaixo são as que batem com a intuição na maioria
 * dos casos:
 *
 *  - último separador seguido de 1 ou 2 dígitos → é o decimal ("54,90");
 *  - último separador seguido de 3 dígitos, e todos os separadores iguais →
 *    é agrupamento de milhar ("1.234", "1.234.567");
 *  - separadores diferentes → o último é o decimal ("1.234,56" e "1,234.56"
 *    dão o mesmo valor).
 *
 * @return centavos, ou null se a entrada não for um valor monetário válido.
 */
fun parseMoneyToCents(input: String): Long? {
    if (input.contains('-')) return null

    val cleaned = input.filter { it.isDigit() || it == '.' || it == ',' }
    if (cleaned.isEmpty() || cleaned.none { it.isDigit() }) return null

    val lastSeparator = cleaned.indexOfLast { it == '.' || it == ',' }
    val digitsAfterSeparator = if (lastSeparator < 0) -1 else cleaned.length - lastSeparator - 1
    val usesASingleSeparatorSymbol = cleaned.none { it == '.' } || cleaned.none { it == ',' }

    val separatorIsDecimal =
        when {
            lastSeparator < 0 -> false
            digitsAfterSeparator == 3 && usesASingleSeparatorSymbol -> false
            else -> true
        }

    val integerText = if (separatorIsDecimal) cleaned.substring(0, lastSeparator) else cleaned
    val decimalText = if (separatorIsDecimal) cleaned.substring(lastSeparator + 1) else ""

    // Depois de escolher o decimal, o que sobrou de separador no lado inteiro
    // só pode ser agrupamento.
    val units = integerText.filter { it.isDigit() }.ifEmpty { "0" }.toLongOrNull() ?: return null
    if (decimalText.length > 2 || decimalText.any { !it.isDigit() }) return null

    val cents = decimalText.padEnd(2, '0').ifEmpty { "00" }.toLongOrNull() ?: return null
    return units * 100 + cents
}

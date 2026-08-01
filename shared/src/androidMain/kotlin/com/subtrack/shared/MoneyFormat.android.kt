package com.subtrack.shared

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency

actual fun formatMoney(
    amountCents: Long,
    currencyCode: String,
): String {
    // valueOf(unscaled, scale) monta o decimal exato a partir dos centavos —
    // sem passar por Double em momento nenhum.
    val amount = BigDecimal.valueOf(amountCents, 2)
    val format = NumberFormat.getCurrencyInstance()
    format.currency = Currency.getInstance(currencyCode)
    return format.format(amount)
}

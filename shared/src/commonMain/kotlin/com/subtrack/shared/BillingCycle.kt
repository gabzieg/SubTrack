package com.subtrack.shared

enum class BillingCycle {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    ;

    companion object {
        fun fromStorage(value: String): BillingCycle = entries.firstOrNull { it.name == value }
            ?: throw IllegalArgumentException("Ciclo de cobrança desconhecido: $value")
    }
}

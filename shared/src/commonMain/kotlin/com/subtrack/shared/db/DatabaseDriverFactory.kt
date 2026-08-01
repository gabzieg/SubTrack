package com.subtrack.shared.db

import app.cash.sqldelight.db.SqlDriver

internal const val DATABASE_NAME = "subtrack.db"

/**
 * Cada plataforma abre o SQLite do seu jeito (Android precisa de Context, iOS
 * usa o driver nativo). O resto do módulo compartilhado só conhece SqlDriver.
 */
expect class DatabaseDriverFactory {
    fun create(): SqlDriver
}

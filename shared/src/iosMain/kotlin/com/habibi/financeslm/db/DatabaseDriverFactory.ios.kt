package com.habibi.financeslm.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.habibi.financeslm.FinanceSlmDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(FinanceSlmDatabase.Schema, "financeslm.db")
    }
}

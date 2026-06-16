package com.habibi.financeslm.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.habibi.financeslm.db.FinanceSlmDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(FinanceSlmDatabase.Schema, context, "financeslm.db")
    }
}

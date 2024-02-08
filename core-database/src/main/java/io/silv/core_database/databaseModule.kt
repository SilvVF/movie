package io.silv.core_database

import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import io.silv.Database
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {

    single<SqlDriver> {
        AndroidSqliteDriver(
            Database.Schema,
            androidContext(),
            "movie.db",
            callback = object : AndroidSqliteDriver.Callback(Database.Schema) {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    db.setForeignKeyConstraintsEnabled(true)
                }
            }
        )
    }

    single {
        Database(driver = get())
    }

    single<DatabaseHandler> {
        DatabaseHandlerImpl(db = get(), driver = get())
    }
}
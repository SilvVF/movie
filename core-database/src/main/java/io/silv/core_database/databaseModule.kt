package io.silv.core_database

import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import io.silv.Database
import io.silv.Movie
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
        Database(
            driver = get(),
            movieAdapter = Movie.Adapter(
                genreIdsAdapter = object: ColumnAdapter<List<Int>, String> {
                    override fun decode(databaseValue: String): List<Int> {
                        return databaseValue.split(",").map { it.toInt() }
                    }

                    override fun encode(value: List<Int>): String {
                        return value.joinToString(",")
                    }
                },
                genresAdapter = object: ColumnAdapter<List<String>, String> {
                    override fun decode(databaseValue: String): List<String> {
                        return databaseValue.split("<|>")
                    }

                    override fun encode(value: List<String>): String {
                        return value.joinToString("<|>")
                    }
                },
            )
        )
    }

    single<DatabaseHandler> {
        DatabaseHandlerImpl(db = get(), driver = get())
    }
}
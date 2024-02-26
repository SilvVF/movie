package io.silv.movie.database

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
                genreIdsAdapter = object : ColumnAdapter<List<Int>, String> {
                    override fun decode(databaseValue: String) =
                        if (databaseValue.isEmpty()) {
                            listOf()
                        } else {
                            databaseValue.split(",").mapNotNull { it.toIntOrNull() }
                        }
                    override fun encode(value: List<Int>) = value.joinToString(separator = ",")
                },
                genresAdapter = object : ColumnAdapter<List<String>, String> {
                    override fun decode(databaseValue: String) =
                        if (databaseValue.isEmpty()) {
                            listOf()
                        } else {
                            databaseValue.split(",")
                        }
                    override fun encode(value: List<String>) = value.joinToString(separator = ",")
                }
            )
        )
    }

    single<DatabaseHandler> {
        DatabaseHandlerImpl(db = get(), driver = get())
    }
}
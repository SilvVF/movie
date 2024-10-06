package io.silv.movie.di

import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import io.silv.Database
import io.silv.Movie
import io.silv.Show
import io.silv.movie.database.DBAdapters.listIntAdapter
import io.silv.movie.database.DBAdapters.listStringAdapter
import io.silv.movie.database.DatabaseHandler
import io.silv.movie.database.DatabaseHandlerImpl
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
                genreIdsAdapter = listIntAdapter,
                genresAdapter = listStringAdapter,
                production_companiesAdapter = listStringAdapter
            ),
            showAdapter = Show.Adapter(
                genre_idsAdapter = listIntAdapter,
                genresAdapter = listStringAdapter,
                production_companiesAdapter = listStringAdapter
            )
        )
    }

    single<DatabaseHandler> {
        DatabaseHandlerImpl(db = get(), driver = get())
    }
}
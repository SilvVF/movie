package io.silv.movie

import io.silv.movie.data.dataModule
import io.silv.movie.presentation.browse.movie.MovieScreenModel
import io.silv.movie.presentation.browse.tv.TVScreenModel
import io.silv.movie.presentation.library.LibraryScreenModel
import io.silv.movie.presentation.view.movie.MovieViewScreenModel
import io.silv.movie.presentation.view.tv.TVViewScreenModel
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val appModule =
    module {

        includes(dataModule)

        factoryOf(::MovieScreenModel)

        factoryOf(::PlayerViewModel)

        factoryOf(::MovieViewScreenModel)

        factoryOf(::TVScreenModel)

        factoryOf(::TVViewScreenModel)

        factoryOf(::LibraryScreenModel)

        single {
            Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            }
        }
    }

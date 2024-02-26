package io.silv.movie

import io.silv.movie.data.dataModule
import io.silv.movie.presentation.movie.browse.MovieScreenModel
import io.silv.movie.presentation.movie.view.MovieViewScreenModel
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val appModule =
    module {

        includes(dataModule)

        factoryOf(::MovieScreenModel)

        factoryOf(::MainViewModel)

        factoryOf(::MovieViewScreenModel)

        single {
            Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            }
        }
    }

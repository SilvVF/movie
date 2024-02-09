package io.silv.movie

import io.silv.data.dataModule
import io.silv.movie.presentation.movie.browse.MovieScreenModel
import io.silv.movie.presentation.movie.discover.MovieDiscoverScreenModel
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val appModule =
    module {

        includes(dataModule)

        factoryOf(::MovieScreenModel)

        factoryOf(::MovieDiscoverScreenModel)

        single {
            Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            }
        }
    }

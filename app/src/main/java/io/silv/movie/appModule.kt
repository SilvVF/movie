package io.silv.movie

import io.silv.core_network.networkModule
import io.silv.data.dataModule
import io.silv.movie.presentation.movie.MovieScreenModel
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val appModule =
    module {

        includes(
            networkModule,
            dataModule
        )

        factoryOf(::MovieScreenModel)

        single {
            Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            }
        }
    }

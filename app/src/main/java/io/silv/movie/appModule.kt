package io.silv.movie

import io.silv.movie.data.dataModule
import io.silv.movie.presentation.media.PlayerViewModel
import io.silv.movie.presentation.movie.browse.MovieScreenModel
import io.silv.movie.presentation.movie.view.MovieViewScreenModel
import io.silv.movie.presentation.tv.browse.TVScreenModel
import io.silv.movie.presentation.tv.view.TVViewScreenModel
import kotlinx.serialization.json.Json
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val appModule =
    module {

        includes(dataModule)

        factoryOf(::MovieScreenModel)

        factoryOf(::MainViewModel)

        factoryOf(::MovieViewScreenModel)

        factoryOf(::TVScreenModel)

        factoryOf(::TVViewScreenModel)

        viewModelOf(::PlayerViewModel)

        single {
            Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            }
        }
    }

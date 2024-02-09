package io.silv.movie

import io.silv.data.dataModule
import io.silv.data.movie.model.Genre
import io.silv.movie.presentation.movie.browse.MovieScreenModel
import io.silv.movie.presentation.movie.browse.Resource
import io.silv.movie.presentation.movie.discover.MovieDiscoverScreenModel
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val appModule =
    module {

        includes(dataModule)

        factoryOf(::MovieScreenModel)

        factory { (genre: Genre?, resource: Resource?) ->
            MovieDiscoverScreenModel(get(), get(), get(), get(), get(), get(), genre, resource)
        }

        single {
            Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            }
        }
    }

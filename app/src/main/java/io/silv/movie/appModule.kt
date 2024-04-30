package io.silv.movie

import io.silv.movie.core.NetworkMonitor
import io.silv.movie.data.dataModule
import io.silv.movie.presentation.covers.ImageSaver
import io.silv.movie.presentation.covers.cache.ListCoverCache
import io.silv.movie.presentation.covers.cache.MovieCoverCache
import io.silv.movie.presentation.covers.cache.ProfileImageCache
import io.silv.movie.presentation.covers.cache.TVShowCoverCache
import io.silv.movie.presentation.screenModelModule
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module


val appModule =
    module {

        singleOf(::MovieCoverCache)

        singleOf(::ProfileImageCache)

        singleOf(::TVShowCoverCache)

        singleOf(::ListCoverCache)

        singleOf(::ImageSaver)

        singleOf(::NetworkMonitor)

        single {
            Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            }
        }

        includes(dataModule)

        includes(screenModelModule)
    }

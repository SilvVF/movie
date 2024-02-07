package io.silv.data

import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val dataModule =
    module {

        factoryOf(::GetMovie)

        factoryOf(::GetRemoteMovie)

        factoryOf(::NetworkToLocalMovie)

        singleOf(::SourceMovieRepositoryImpl) { bind<SourceMovieRepository>() }

        singleOf(::MovieRepositoryImpl) { bind<MovieRepository>() }
}
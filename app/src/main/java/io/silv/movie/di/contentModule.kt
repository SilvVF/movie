package io.silv.movie.di

import io.silv.movie.data.content.movie.local.CreditRepository
import io.silv.movie.data.content.movie.local.CreditRepositoryImpl
import io.silv.movie.data.content.movie.network.SourceCreditsRepository
import io.silv.movie.data.content.lists.ContentListRepository
import io.silv.movie.data.content.lists.ContentListRepositoryImpl
import io.silv.movie.data.content.movie.local.LocalContentDelegate
import io.silv.movie.data.content.movie.local.LocalContentRepositoryImpl
import io.silv.movie.data.content.movie.local.MovieRepository
import io.silv.movie.data.content.movie.local.MovieRepositoryImpl
import io.silv.movie.data.content.movie.network.SourceMovieRepository
import io.silv.movie.data.content.movie.network.SourceMovieRepositoryImpl
import io.silv.movie.data.content.movie.network.SourceTrailerRepository
import io.silv.movie.data.content.movie.local.TrailerRepository
import io.silv.movie.data.content.movie.local.TrailerRepositoryImpl
import io.silv.movie.data.content.movie.local.ShowRepository
import io.silv.movie.data.content.movie.local.ShowRepositoryImpl
import io.silv.movie.data.content.movie.network.NetworkContentDelegate
import io.silv.movie.data.content.movie.network.NetworkContentDelegateImpl
import io.silv.movie.data.content.movie.network.SourceShowRepository
import io.silv.movie.data.content.movie.network.SourceShowRepositoryImpl
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val contentModule = module {

    factoryOf(::SourceTrailerRepository)

    singleOf(::CreditRepositoryImpl) { bind<CreditRepository>() }

    singleOf(::ContentListRepositoryImpl) { bind<ContentListRepository>() }

    singleOf(::TrailerRepositoryImpl) { bind<TrailerRepository>() }

    singleOf(::SourceMovieRepositoryImpl) { bind<SourceMovieRepository>() }

    singleOf(::SourceShowRepositoryImpl) { bind<SourceShowRepository>() }

    singleOf(::MovieRepositoryImpl) { bind<MovieRepository>() }

    singleOf(::ShowRepositoryImpl) { bind<ShowRepository>() }

    factoryOf(::LocalContentRepositoryImpl) { bind<LocalContentDelegate>() }

    factoryOf(::NetworkContentDelegateImpl) { bind<NetworkContentDelegate>() }

    factoryOf(::SourceCreditsRepository)
}

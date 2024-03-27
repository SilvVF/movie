package io.silv.movie

import io.silv.core_ui.voyager.ScreenResultsViewModel
import io.silv.movie.core.NetworkMonitor
import io.silv.movie.data.cache.ListCoverCache
import io.silv.movie.data.cache.MovieCoverCache
import io.silv.movie.data.cache.ProfileImageCache
import io.silv.movie.data.cache.TVShowCoverCache
import io.silv.movie.data.dataModule
import io.silv.movie.data.recommendation.RecommendationManager
import io.silv.movie.presentation.browse.movie.MovieScreenModel
import io.silv.movie.presentation.browse.tv.TVScreenModel
import io.silv.movie.presentation.library.screenmodels.FavoritesScreenModel
import io.silv.movie.presentation.library.screenmodels.LibraryScreenModel
import io.silv.movie.presentation.library.screenmodels.ListAddScreenModel
import io.silv.movie.presentation.library.screenmodels.ListCoverScreenModel
import io.silv.movie.presentation.library.screenmodels.ListViewScreenModel
import io.silv.movie.presentation.profile.ProfileScreenModel
import io.silv.movie.presentation.profile.screen.SelectProfileImageScreenModel
import io.silv.movie.presentation.view.CreditsViewScreenModel
import io.silv.movie.presentation.view.ImageSaver
import io.silv.movie.presentation.view.MovieCoverScreenModel
import io.silv.movie.presentation.view.TVCoverScreenModel
import io.silv.movie.presentation.view.movie.MovieViewScreenModel
import io.silv.movie.presentation.view.tv.TVViewScreenModel
import kotlinx.serialization.json.Json
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val appModule =
    module {

        includes(dataModule)

        factoryOf(::MovieScreenModel)

        viewModelOf(::PlayerViewModel)

        factoryOf(::MovieViewScreenModel)

        factoryOf(::TVScreenModel)

        factoryOf(::FavoritesScreenModel)

        factoryOf(::TVViewScreenModel)

        factoryOf(::LibraryScreenModel)

        factoryOf(::ListViewScreenModel)

        factoryOf(::SelectProfileImageScreenModel)

        factoryOf(::ListAddScreenModel)

        factoryOf(::MovieCoverScreenModel)

        factoryOf(::CreditsViewScreenModel)

        factoryOf(::ProfileScreenModel)

        factoryOf(::TVCoverScreenModel)

        factoryOf(::ListCoverScreenModel)

        singleOf(::MovieCoverCache)

        singleOf(::ProfileImageCache)

        singleOf(::TVShowCoverCache)

        singleOf(::ListCoverCache)

        singleOf(::ImageSaver)

        singleOf(::RecommendationManager)

        singleOf(::NetworkMonitor)

        viewModelOf(::ScreenResultsViewModel)

        single {
            Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            }
        }
    }

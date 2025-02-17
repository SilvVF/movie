package io.silv.movie.di

import io.silv.core_ui.voyager.ScreenResultsViewModel
import io.silv.movie.MainViewModel
import io.silv.movie.presentation.components.profile.ProfileScreenModel
import io.silv.movie.presentation.covers.screenmodel.ListCoverScreenModel
import io.silv.movie.presentation.covers.screenmodel.MovieCoverScreenModel
import io.silv.movie.presentation.covers.screenmodel.TVCoverScreenModel
import io.silv.movie.presentation.media.PlayerViewModel
import io.silv.movie.presentation.screen.ProfileViewScreenModel
import io.silv.movie.presentation.screenmodel.AddToListScreenModel
import io.silv.movie.presentation.screenmodel.BrowseListsScreenModel
import io.silv.movie.presentation.screenmodel.CommentsScreenModel
import io.silv.movie.presentation.screenmodel.CreditsViewScreenModel
import io.silv.movie.presentation.screenmodel.FavoritesScreenModel
import io.silv.movie.presentation.screenmodel.LibraryScreenModel
import io.silv.movie.presentation.screenmodel.ListAddScreenModel
import io.silv.movie.presentation.screenmodel.ListPagedScreenModel
import io.silv.movie.presentation.screenmodel.ListViewScreenModel
import io.silv.movie.presentation.screenmodel.MovieScreenModel
import io.silv.movie.presentation.screenmodel.MovieViewScreenModel
import io.silv.movie.presentation.screenmodel.PersonViewScreenModel
import io.silv.movie.presentation.screenmodel.SearchForListScreenModel
import io.silv.movie.presentation.screenmodel.SelectProfileImageScreenModel
import io.silv.movie.presentation.screenmodel.TVScreenModel
import io.silv.movie.presentation.screenmodel.TVViewScreenModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val screenModelModule = module {

        viewModelOf(::MainViewModel)

        viewModelOf(::ScreenResultsViewModel)

        factoryOf(::MovieScreenModel)

        viewModelOf(::PlayerViewModel)

        factoryOf(::MovieViewScreenModel)

        factoryOf(::TVScreenModel)

        factoryOf(::FavoritesScreenModel)

        factoryOf(::TVViewScreenModel)

        factoryOf(::LibraryScreenModel)

        factoryOf(::ListViewScreenModel)

        factoryOf(::ListAddScreenModel)

        factoryOf(::MovieCoverScreenModel)

        factoryOf(::CreditsViewScreenModel)

        factoryOf(::CommentsScreenModel)

        factoryOf(::ProfileScreenModel)

        factoryOf(::TVCoverScreenModel)

        factoryOf(::ListCoverScreenModel)

        factoryOf(::BrowseListsScreenModel)

        factoryOf(::PersonViewScreenModel)

        factoryOf(::AddToListScreenModel)

        factoryOf(::SearchForListScreenModel)

        factoryOf(::ListPagedScreenModel)

        factoryOf(::SelectProfileImageScreenModel)

        factoryOf(::ProfileViewScreenModel)
}
package io.silv.movie.presentation

import io.silv.core_ui.voyager.ScreenResultsViewModel
import io.silv.movie.MainScreenModel
import io.silv.movie.presentation.content.screenmodel.CommentsScreenModel
import io.silv.movie.presentation.content.screenmodel.CreditsViewScreenModel
import io.silv.movie.presentation.content.screenmodel.MovieScreenModel
import io.silv.movie.presentation.content.screenmodel.MovieViewScreenModel
import io.silv.movie.presentation.content.screenmodel.PersonViewScreenModel
import io.silv.movie.presentation.content.screenmodel.TVScreenModel
import io.silv.movie.presentation.content.screenmodel.TVViewScreenModel
import io.silv.movie.presentation.covers.screenmodel.ListCoverScreenModel
import io.silv.movie.presentation.covers.screenmodel.MovieCoverScreenModel
import io.silv.movie.presentation.covers.screenmodel.TVCoverScreenModel
import io.silv.movie.presentation.list.screenmodel.BrowseListsScreenModel
import io.silv.movie.presentation.list.screenmodel.FavoritesScreenModel
import io.silv.movie.presentation.list.screenmodel.LibraryScreenModel
import io.silv.movie.presentation.list.screenmodel.ListAddScreenModel
import io.silv.movie.presentation.list.screenmodel.ListPagedScreenModel
import io.silv.movie.presentation.list.screenmodel.ListViewScreenModel
import io.silv.movie.presentation.list.screenmodel.SearchForListScreenModel
import io.silv.movie.presentation.media.PlayerViewModel
import io.silv.movie.presentation.profile.ProfileScreenModel
import io.silv.movie.presentation.result.screenmodel.AddToListScreenModel
import io.silv.movie.presentation.result.screenmodel.SelectProfileImageScreenModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val screenModelModule = module {

    viewModelOf(::MainScreenModel)

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
}
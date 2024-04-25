package io.silv.movie.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import io.silv.movie.data.credits.CreditRepository
import io.silv.movie.data.credits.CreditRepositoryImpl
import io.silv.movie.data.credits.GetMovieCredits
import io.silv.movie.data.credits.GetRemoteCredits
import io.silv.movie.data.credits.GetTVShowCredits
import io.silv.movie.data.credits.NetworkToLocalCredit
import io.silv.movie.data.lists.ContentListRepository
import io.silv.movie.data.lists.ContentListRepositoryImpl
import io.silv.movie.data.lists.GetFavoritesList
import io.silv.movie.data.lists.interactor.AddContentItemToList
import io.silv.movie.data.lists.interactor.DeleteContentList
import io.silv.movie.data.lists.interactor.EditContentList
import io.silv.movie.data.lists.interactor.RemoveContentItemFromList
import io.silv.movie.data.lists.interactor.ToggleContentItemFavorite
import io.silv.movie.data.movie.interactor.GetMovie
import io.silv.movie.data.movie.interactor.GetRemoteMovie
import io.silv.movie.data.movie.interactor.NetworkToLocalMovie
import io.silv.movie.data.movie.interactor.UpdateMovie
import io.silv.movie.data.movie.repository.MovieRepository
import io.silv.movie.data.movie.repository.MovieRepositoryImpl
import io.silv.movie.data.movie.repository.SourceMovieRepository
import io.silv.movie.data.movie.repository.SourceMovieRepositoryImpl
import io.silv.movie.data.prefrences.BasePreferences
import io.silv.movie.data.prefrences.BrowsePreferences
import io.silv.movie.data.prefrences.LibraryPreferences
import io.silv.movie.data.prefrences.UiPreferences
import io.silv.movie.data.prefrences.core.DatastorePreferenceStore
import io.silv.movie.data.prefrences.core.PreferenceStore
import io.silv.movie.data.recommendation.RecommendationWorker
import io.silv.movie.data.trailers.GetMovieTrailers
import io.silv.movie.data.trailers.GetRemoteTrailers
import io.silv.movie.data.trailers.GetTVShowTrailers
import io.silv.movie.data.trailers.NetworkToLocalTrailer
import io.silv.movie.data.trailers.TrailerRepository
import io.silv.movie.data.trailers.TrailerRepositoryImpl
import io.silv.movie.data.tv.interactor.GetRemoteTVShows
import io.silv.movie.data.tv.interactor.GetShow
import io.silv.movie.data.tv.interactor.NetworkToLocalTVShow
import io.silv.movie.data.tv.interactor.UpdateShow
import io.silv.movie.data.tv.repository.ShowRepository
import io.silv.movie.data.tv.repository.ShowRepositoryImpl
import io.silv.movie.data.tv.repository.SourceTVRepository
import io.silv.movie.data.tv.repository.SourceTVRepositoryImpl
import io.silv.movie.data.user.FavoritesUpdateManager
import io.silv.movie.data.user.FavoritesUpdateWorker
import io.silv.movie.data.user.ListRepository
import io.silv.movie.data.user.ListUpdateManager
import io.silv.movie.data.user.ListUpdateWorker
import io.silv.movie.data.user.ListUpdater
import io.silv.movie.data.user.UserListUpdateManager
import io.silv.movie.data.user.UserListUpdateWorker
import io.silv.movie.data.user.UserRepository
import io.silv.movie.data.user.UserRepositoryImpl
import io.silv.movie.database.databaseModule
import io.silv.movie.network.networkModule
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val dataModule =
    module {

        includes(databaseModule)

        includes(networkModule)

        factoryOf(::GetMovie)

        factoryOf(::UpdateShow)

        factoryOf(::GetShow)

        factoryOf(::GetFavoritesList)

        factoryOf(::UpdateMovie)

        factoryOf(::NetworkToLocalTVShow)

        factoryOf(::GetRemoteMovie)

        factoryOf(::GetRemoteTVShows)

        factoryOf(::GetTVShowTrailers)

        factoryOf(::GetMovieTrailers)

        factoryOf(::NetworkToLocalMovie)

        factoryOf(::NetworkToLocalTrailer)

        factoryOf(::NetworkToLocalCredit)

        factoryOf(::GetRemoteCredits)

        factoryOf(::GetMovieCredits)

        factoryOf(::GetTVShowCredits)

        factoryOf(::GetRemoteTrailers)

        singleOf(::CreditRepositoryImpl) { bind<CreditRepository>() }

        singleOf(::ContentListRepositoryImpl) { bind<ContentListRepository>() }

        singleOf(::TrailerRepositoryImpl) { bind<TrailerRepository>() }

        singleOf(::SourceMovieRepositoryImpl) { bind<SourceMovieRepository>() }

        singleOf(::SourceTVRepositoryImpl) { bind<SourceTVRepository>() }

        singleOf(::MovieRepositoryImpl) { bind<MovieRepository>() }

        singleOf(::ShowRepositoryImpl) { bind<ShowRepository>() }

        singleOf(::UserRepositoryImpl) { bind<UserRepository>() }

        singleOf(::ListRepository)

        singleOf(::BrowsePreferences)

        singleOf(::LibraryPreferences)

        singleOf(::BasePreferences)

        single<PreferenceStore> {
            DatastorePreferenceStore(androidContext().dataStore)
        }

        singleOf(::UiPreferences)

        workerOf(::RecommendationWorker)

        workerOf(::FavoritesUpdateWorker)

        workerOf(::UserListUpdateWorker)

        workerOf(::ListUpdateWorker)

        factoryOf(::ListUpdater)

        singleOf(::ListUpdateManager)

        singleOf(::FavoritesUpdateManager)

        singleOf(::UserListUpdateManager)

        factoryOf(::EditContentList)

        factoryOf(::AddContentItemToList)

        factoryOf(::RemoveContentItemFromList)

        factoryOf(::DeleteContentList)

        factoryOf(::ToggleContentItemFavorite)
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
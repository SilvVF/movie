package io.silv.movie.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import io.silv.movie.data.movie.interactor.GetMovie
import io.silv.movie.data.movie.interactor.GetRemoteMovie
import io.silv.movie.data.movie.interactor.GetRemoteTVShows
import io.silv.movie.data.movie.interactor.NetworkToLocalMovie
import io.silv.movie.data.movie.interactor.UpdateMovie
import io.silv.movie.data.movie.repository.MovieRepository
import io.silv.movie.data.movie.repository.MovieRepositoryImpl
import io.silv.movie.data.movie.repository.SourceMovieRepository
import io.silv.movie.data.movie.repository.SourceMovieRepositoryImpl
import io.silv.movie.data.movie.repository.SourceTVRepository
import io.silv.movie.data.movie.repository.SourceTVRepositoryImpl
import io.silv.movie.data.prefrences.TMDBPreferences
import io.silv.movie.data.prefrences.core.DatastorePreferenceStore
import io.silv.movie.data.prefrences.core.PreferenceStore
import io.silv.movie.data.trailers.GetMovieTrailers
import io.silv.movie.data.trailers.GetRemoteTrailers
import io.silv.movie.data.trailers.GetTVShowTrailers
import io.silv.movie.data.trailers.NetworkToLocalTrailer
import io.silv.movie.data.trailers.TrailerRepository
import io.silv.movie.data.trailers.TrailerRepositoryImpl
import io.silv.movie.data.tv.interactor.GetShow
import io.silv.movie.data.tv.interactor.NetworkToLocalTVShow
import io.silv.movie.data.tv.interactor.UpdateShow
import io.silv.movie.data.tv.repository.ShowRepository
import io.silv.movie.data.tv.repository.ShowRepositoryImpl
import io.silv.movie.network.networkModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val dataModule =
    module {

        includes(io.silv.movie.database.databaseModule)
        includes(networkModule)

        factoryOf(::GetMovie)

        factoryOf(::UpdateShow)

        factoryOf(::GetShow)

        factoryOf(::UpdateMovie)

        factoryOf(::NetworkToLocalTVShow)

        factoryOf(::GetRemoteMovie)

        factoryOf(::GetRemoteTVShows)

        factoryOf(::GetTVShowTrailers)

        factoryOf(::GetMovieTrailers)

        factoryOf(::NetworkToLocalMovie)

        factoryOf(::NetworkToLocalTrailer)

        factoryOf(::GetRemoteTrailers)

        singleOf(::TrailerRepositoryImpl) { bind<TrailerRepository>() }

        singleOf(::SourceMovieRepositoryImpl) { bind<SourceMovieRepository>() }

        singleOf(::SourceTVRepositoryImpl) { bind<SourceTVRepository>() }

        singleOf(::MovieRepositoryImpl) { bind<MovieRepository>() }

        singleOf(::ShowRepositoryImpl) { bind<ShowRepository>() }

        singleOf(::TMDBPreferences)

        single<PreferenceStore> {
            DatastorePreferenceStore(androidContext().dataStore)
        }
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
package io.silv.movie.di

import io.silv.movie.data.content.lists.RecommendationManager
import io.silv.movie.data.content.lists.RecommendationWorker
import io.silv.movie.data.content.lists.interactor.DeleteContentList
import io.silv.movie.data.content.lists.interactor.EditContentList
import io.silv.movie.data.prefrences.StoragePreferences
import io.silv.movie.data.prefrences.UiPreferences
import io.silv.movie.data.prefrences.preferenceModule
import io.silv.movie.data.user.FavoritesUpdateManager
import io.silv.movie.data.user.ListUpdateManager
import io.silv.movie.data.user.UserListUpdateManager
import io.silv.movie.data.user.repository.CommentsRepository
import io.silv.movie.data.user.repository.ListRepository
import io.silv.movie.data.user.repository.UserRepository
import io.silv.movie.data.user.repository.UserRepositoryImpl
import io.silv.movie.data.user.worker.FavoritesUpdateWorker
import io.silv.movie.data.user.worker.ListUpdateWorker
import io.silv.movie.data.user.worker.ListUpdater
import io.silv.movie.data.user.worker.UserListUpdateWorker
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val dataModule =
    module {

        singleOf(::UserRepositoryImpl) { bind<UserRepository>() }

        singleOf(::ListRepository)

        singleOf(::CommentsRepository)

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

        factoryOf(::DeleteContentList)

        singleOf(::RecommendationManager)

        singleOf(::StoragePreferences)

        includes(databaseModule)

        includes(networkModule)

        includes(contentModule)

        includes(preferenceModule)
}

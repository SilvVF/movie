package io.silv.movie.data

import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import io.silv.movie.data.workers.ListUpdateWorker
import io.silv.movie.data.workers.FavoritesUpdateWorker
import io.silv.movie.data.workers.UserListUpdateWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext



class ListUpdateManager(
    private val context: Context,
) {

    private val workManager = WorkManager.getInstance(context)

    fun isRunning(listId: String): Flow<Boolean> = workManager
            .getWorkInfosForUniqueWorkFlow(ListUpdateWorker.BASE_TAG + listId)
            .map { workInfos ->
                workInfos.any { info -> info.state == WorkInfo.State.RUNNING }
            }

    fun refreshList(listId: String) {
        ListUpdateWorker.start(context, listId)
    }

    suspend fun awaitRefresh(listId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching<Unit> {
                ListUpdateWorker.start(context, listId).await()
            }
        }

    fun isFavoritesUpdateRunning(): Flow<Boolean> = workManager
        .getWorkInfosForUniqueWorkFlow(FavoritesUpdateWorker.TAG)
        .map { workInfos ->
            workInfos.any { info -> info.state == WorkInfo.State.RUNNING }
        }

    fun refreshFavorites() {
        FavoritesUpdateWorker.start(context)
    }

    fun isUserListUpdateRunning(): Flow<Boolean> = workManager
        .getWorkInfosForUniqueWorkFlow(UserListUpdateWorker.TAG)
        .map { workInfos ->
            workInfos.any { info -> info.state == WorkInfo.State.RUNNING }
        }

    fun refreshUserLists() {
        UserListUpdateWorker.start(context)
    }
}
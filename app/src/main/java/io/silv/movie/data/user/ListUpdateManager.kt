package io.silv.movie.data.user

import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ListUpdateManager(
    private val context: Context,
    private val listUpdater: ListUpdater,
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

    suspend fun awaitRefresh(listId: String) =
        withContext(Dispatchers.IO) {
            runCatching {
                listUpdater.await(listId)
            }
        }
}
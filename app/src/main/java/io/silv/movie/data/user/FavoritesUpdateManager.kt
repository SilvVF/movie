package io.silv.movie.data.user

import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager
import io.silv.movie.data.user.worker.FavoritesUpdateWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FavoritesUpdateManager(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)

    fun isRunning(): Flow<Boolean> = workManager
            .getWorkInfosForUniqueWorkFlow(FavoritesUpdateWorker.TAG)
            .map { workInfos ->
                workInfos.any { info -> info.state == WorkInfo.State.RUNNING }
            }

    fun refreshFavorites() {
        FavoritesUpdateWorker.start(context)
    }
}
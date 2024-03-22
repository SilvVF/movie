package io.silv.movie.data.user

import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserListUpdateManager(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)

    fun isRunning(): Flow<Boolean> = workManager
            .getWorkInfosForUniqueWorkFlow(UserListUpdateWorker.TAG)
            .map { workInfos ->
                workInfos.any { info -> info.state == WorkInfo.State.RUNNING }
            }

    fun refreshUserLists() {
        UserListUpdateWorker.start(context)
    }
}
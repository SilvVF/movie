package io.silv.movie.data.recommendation

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import io.silv.movie.data.lists.ContentItem
import io.silv.movie.data.lists.ContentListMapper
import io.silv.movie.database.DatabaseHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RecommendationManager(
    context: Context,
    private val handler: DatabaseHandler
) {
    private val workManager = WorkManager.getInstance(context)

    fun isRunning(id: Long): Flow<Boolean> = workManager
            .getWorkInfosForUniqueWorkFlow(RecommendationWorker.WorkInfoTag + id)
            .map { workInfos ->
                workInfos.any { info -> info.state == WorkInfo.State.RUNNING }
            }

    fun subscribe(id: Long = -1L): Flow<List<ContentItem>> {
        return handler.subscribeToList {
            recommendationQueries.selectRecommendationContentByListId(id, ContentListMapper.mapRecommendation)
        }
    }

    fun refreshListRecommendations(listId: Long) {
        workManager.enqueueUniqueWork(
            RecommendationWorker.WorkInfoTag + listId,
            ExistingWorkPolicy.KEEP,
            RecommendationWorker.workRequest(listId)
        )
    }

    fun refreshFavoritesRecommendations() {
        workManager.enqueueUniqueWork(
            RecommendationWorker.WorkInfoTag,
            ExistingWorkPolicy.KEEP,
            RecommendationWorker.workRequest(-1L)
        )
    }
}
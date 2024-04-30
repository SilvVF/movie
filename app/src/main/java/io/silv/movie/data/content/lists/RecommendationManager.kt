package io.silv.movie.data.content.lists

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import io.silv.movie.data.content.lists.RecommendationWorker.Companion.DEFAULT_MAX_SIZE
import io.silv.movie.data.content.lists.RecommendationWorker.Companion.DEFAULT_MIN_TAKE_SIZE
import io.silv.movie.database.DatabaseHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

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
            .distinctUntilChanged()
            .onEach {
                if (it.isEmpty()) {
                    refreshListRecommendations(id)
                }
            }
    }

    fun refreshListRecommendations(listId: Long, amount: Int = DEFAULT_MAX_SIZE, perItem: Int = DEFAULT_MIN_TAKE_SIZE) {
        workManager.enqueueUniqueWork(
            RecommendationWorker.WorkInfoTag + listId,
            ExistingWorkPolicy.KEEP,
            RecommendationWorker.workRequest(listId, amount, perItem)
        )
    }

    fun refreshFavoritesRecommendations(amount: Int = DEFAULT_MAX_SIZE, perItem: Int = DEFAULT_MIN_TAKE_SIZE) {
        workManager.enqueueUniqueWork(
            RecommendationWorker.WorkInfoTag,
            ExistingWorkPolicy.KEEP,
            RecommendationWorker.workRequest(-1L, amount, perItem)
        )
    }
}
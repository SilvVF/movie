package io.silv.movie.data

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import io.silv.movie.data.model.ContentItem
import io.silv.movie.data.local.ContentListMapper
import io.silv.movie.data.local.ContentListRepository
import io.silv.movie.data.supabase.ListRepository
import io.silv.movie.data.workers.RecommendationWorker
import io.silv.movie.data.workers.RecommendationWorker.Companion.DEFAULT_MAX_SIZE
import io.silv.movie.data.workers.RecommendationWorker.Companion.DEFAULT_MIN_TAKE_SIZE
import io.silv.movie.database.DatabaseHandler
import io.silv.movie.presentation.screenmodel.ListSortMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach



class RecommendationManager(
    context: Context,
    private val contentListRepository: ContentListRepository,
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
            recommendationQueries.selectRecommendationContentByListId(id,
                ContentListMapper.mapRecommendation
            )
        }
            .combine(
                contentListRepository.observeListItemsByListId(id),
            ) { recs, list ->
                if (list.isEmpty()) {
                    recs
                } else {
                    val set = list.map { it.itemKey }.toSet()
                    recs.filterNot { r -> r.itemKey in set }
                }
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
            ExistingWorkPolicy.REPLACE,
            RecommendationWorker.workRequest(listId, amount, perItem)
        )
    }

    fun refreshFavoritesRecommendations(amount: Int = DEFAULT_MAX_SIZE, perItem: Int = DEFAULT_MIN_TAKE_SIZE) {
        workManager.enqueueUniqueWork(
            RecommendationWorker.WorkInfoTag,
            ExistingWorkPolicy.REPLACE,
            RecommendationWorker.workRequest(-1L, amount, perItem)
        )
    }
}
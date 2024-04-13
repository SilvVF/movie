package io.silv.movie.data.recommendation

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.silv.movie.R
import io.silv.movie.core.SMovie
import io.silv.movie.core.STVShow
import io.silv.movie.core.await
import io.silv.movie.data.lists.ContentListMapper
import io.silv.movie.data.movie.interactor.NetworkToLocalMovie
import io.silv.movie.data.movie.model.toDomain
import io.silv.movie.data.tv.interactor.NetworkToLocalTVShow
import io.silv.movie.data.tv.model.toDomain
import io.silv.movie.database.DatabaseHandler
import io.silv.movie.network.model.toSMovie
import io.silv.movie.network.model.toSTVShow
import io.silv.movie.network.service.tmdb.TMDBRecommendationService
import timber.log.Timber
import kotlin.math.ceil
import kotlin.math.roundToInt

class RecommendationWorker(
    private val recommendationService: TMDBRecommendationService,
    private val handler: DatabaseHandler,
    private val networkToLocalTVShow: NetworkToLocalTVShow,
    private val networkToLocalMovie: NetworkToLocalMovie,
    appContext: Context,
    private val params: WorkerParameters
): CoroutineWorker(appContext, params) {

    private suspend fun mostRecentFavorites() =
        handler.awaitList {
            favoritesViewQueries.favoritesOrderByRecent("", ContentListMapper.mapFavoriteItem)
        }


    private suspend fun mostRecentForList(id: Long) =
        handler.awaitList {
            contentItemQueries.selectByListId(id, "", "", ContentListMapper.mapItem)
        }

    override suspend fun doWork(): Result {

        val listId = inputData.getLong(KEY_LIST_ID, -1L)
        val maxSize = inputData.getInt(KEY_MAX_SIZE, DEFAULT_MAX_SIZE)
        val minTakeSize = inputData.getInt(KEY_TAKE_SIZE, DEFAULT_MIN_TAKE_SIZE)

        val content = if (listId == -1L) {
            Timber.d("using favorites")
            mostRecentFavorites()
        } else {
            Timber.d("trying most recent for list")
            mostRecentForList(listId).ifEmpty {
                Timber.d("using favorites")
                mostRecentFavorites()
            }
        }
        val movieIds = content.filter { it.isMovie }.map { it.contentId }
        val showIds = content.filterNot { it.isMovie }.map { it.contentId }

        Timber.d("showIds: ${showIds.size} movieIds: ${movieIds.size}")
        Timber.d(listId.toString())
        Timber.d(content.toString())

        val takeFromEach = (ceil(maxSize.toDouble() / content.size).roundToInt()).coerceAtLeast(minTakeSize)
        var taken = 0
        Timber.d("takeFromEach: $takeFromEach")

        val recommendedMovieIds = mutableSetOf<Long>()
        val recommendedShowIds = mutableSetOf<Long>()

        handler.await(inTransaction = true) {

            recommendationQueries.clearByListId(listId)
            Timber.d("Cleared old list")

            for (item in content) {
                if (taken == maxSize) { break }

                if (item.isMovie)  {
                    val movieRecommendations = getMovieRecommendations(item.contentId)
                        .filter { it.id !in movieIds && recommendedMovieIds.add(it.id) }
                        .take(takeFromEach)

                    for (smovie in movieRecommendations) {
                        if (taken == maxSize) { break }

                        val movie = networkToLocalMovie.await(smovie.toDomain())
                        recommendationQueries.insert(listId, movie.id, null)
                        Timber.d("inserted recommendation movie ${movie.id}")
                        taken++
                    }
                } else {

                    val showRecommendations = getShowRecommendations(item.contentId)
                        .filter { it.id !in showIds && recommendedShowIds.add(it.id) }
                        .take(takeFromEach)

                    for (sshow in showRecommendations) {
                        if (taken == maxSize) { break }

                        val show = networkToLocalTVShow.await(sshow.toDomain())
                        recommendationQueries.insert(listId,null, show.id)
                        Timber.d("inserted recommendation show ${show.id}")
                        taken++
                    }
                }
            }
        }

        return Result.success()
    }

    private suspend fun getMovieRecommendations(id: Long): List<SMovie> {
        return recommendationService.movieRecommendations(id.toInt())
            .await()
            .body()!!
            .results
            .map { it.toSMovie() }
    }

    private suspend fun getShowRecommendations(id: Long): List<STVShow> {
        return recommendationService.showRecommendations(id.toInt())
            .await()
            .body()!!
            .results
            .map { it.toSTVShow() }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo()
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val id = "io.silv.RecommendationWorker"
        val title = "Refreshing recommendations"
        val cancel = "cancel"
        // This PendingIntent can be used to cancel the worker
        val intent = WorkManager.getInstance(applicationContext)
            .createCancelPendingIntent(getId())

        val notification = NotificationCompat.Builder(applicationContext, id)
            .setContentTitle(title)
            .setContentText("refreshing recommendations")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            // Add the cancel action to the notification which can
            // be used to cancel the worker
            .addAction(android.R.drawable.ic_delete, cancel, intent)
            .build()

        return ForegroundInfo(1121, notification)
    }

    companion object  {
        const val WorkInfoTag = "io.silv.RecommendationWorker"
        private const val KEY_LIST_ID = "key_list_id"
        private const val KEY_MAX_SIZE = "key_take_size"
        private const val KEY_TAKE_SIZE = "key_max_size"
        const val DEFAULT_MAX_SIZE = 20
        const val DEFAULT_MIN_TAKE_SIZE = 3

        fun workRequest(
            listId: Long? = null,
            maxSize: Int = DEFAULT_MAX_SIZE,
            takeSize: Int = DEFAULT_MIN_TAKE_SIZE,
        ) = OneTimeWorkRequestBuilder<RecommendationWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInputData(
                Data.Builder()
                    .putLong(KEY_LIST_ID, listId ?: -1L)
                    .putInt(KEY_TAKE_SIZE, takeSize)
                    .putInt(KEY_MAX_SIZE, maxSize)
                    .build()
            )
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag(WorkInfoTag)
            .build()
    }
}

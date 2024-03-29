package io.silv.movie.data.user

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.jan.supabase.gotrue.Auth
import io.silv.movie.R
import io.silv.movie.data.lists.ContentListRepository
import io.silv.movie.data.lists.toUpdate
import io.silv.movie.data.movie.interactor.GetMovie
import io.silv.movie.data.movie.interactor.GetRemoteMovie
import io.silv.movie.data.movie.interactor.NetworkToLocalMovie
import io.silv.movie.data.movie.model.toDomain
import io.silv.movie.data.tv.interactor.GetRemoteTVShows
import io.silv.movie.data.tv.interactor.GetShow
import io.silv.movie.data.tv.interactor.NetworkToLocalTVShow
import io.silv.movie.data.tv.model.toDomain
import kotlinx.datetime.Clock
import timber.log.Timber

/**
 * Toggles favorite for movies and shows from the list in supabase.
 * Fetches the movie or show if it does not exist already.
 * No deletion / removals of favorites happen only additions for current signed in user.
 */
class ListUpdateWorker (
    private val listUpdater: ListUpdater,

    appContext: Context,
    private val params: WorkerParameters
): CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {

        listUpdater.await(inputData.getString(KEY_LIST_ID)!!)

        return Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo(inputData.getString(KEY_LIST_ID).orEmpty())
    }

    private fun createForegroundInfo(listId: String): ForegroundInfo {
        val id = BASE_TAG + listId
        val title = "Refreshing list $listId"
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

        return ForegroundInfo(1133, notification)
    }


    companion object {
        const val BASE_TAG = "io.silv.ListUpdateWorker"
        const val KEY_LIST_ID = "list_id"

        fun start(context: Context, listId: String) {
            val workManager = WorkManager.getInstance(context)

            workManager.enqueueUniqueWork(
                BASE_TAG + listId,
                ExistingWorkPolicy.REPLACE,
                workRequest(listId)
            )
        }

        fun workRequest(listId: String) = OneTimeWorkRequestBuilder<ListUpdateWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInputData(
                Data.Builder()
                    .putString(KEY_LIST_ID, listId)
                    .build()
            )
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag(BASE_TAG + listId)
            .build()
    }
}

class ListUpdater(
    private val contentListRepository: ContentListRepository,
    private val listRepository: ListRepository,
    private val getShow: GetShow,
    private val getMovie: GetMovie,
    private val getRemoteTVShows: GetRemoteTVShows,
    private val getRemoteMovie: GetRemoteMovie,
    private val networkToLocalMovie: NetworkToLocalMovie,
    private val networkToLocalTVShow: NetworkToLocalTVShow,
    private val userRepository: UserRepository,
    private val auth: Auth,
) {

    suspend fun await(listId: String) {

        val list = listRepository.selectListWithItemsById(listId)!!
        val username = userRepository.getUser(list.userId)?.username.orEmpty()
        val isOwnerMe = list.userId == auth.currentUserOrNull()?.id

        try {
            var local = contentListRepository.getListForSupabaseId(list.listId)

            if (local == null) {
                val id = contentListRepository.createList(
                    name = list.name,
                    supabaseId = list.listId,
                    userId = list.userId,
                    createdAt = list.createdAt.toEpochMilliseconds(),
                    inLibrary = isOwnerMe
                )
                local = contentListRepository.getList(id)!!
            }

            contentListRepository.updateList(
                local.copy(
                    description = list.description,
                    lastModified = list.updatedAt?.toEpochMilliseconds() ?: local.lastModified,
                    name = list.name,
                    lastSynced = Clock.System.now().toEpochMilliseconds(),
                    username = username,

                )
                    .toUpdate()
            )

            val items = list.items.orEmpty()

            for (item in items) {
                if (item.movieId != -1L) {
                    var movie = getMovie.await(item.movieId)
                    if (movie == null) {
                        movie = networkToLocalMovie.await(
                            getRemoteMovie.awaitOne(item.movieId)!!.toDomain()
                        )
                    }
                    contentListRepository.addMovieToList(movie.id, local)
                } else if (item.showId != -1L) {
                    var show = getShow.await(item.showId)
                    if (show == null) {
                        show = networkToLocalTVShow.await(
                            getRemoteTVShows.awaitOne(item.showId)!!.toDomain()
                        )
                    }
                    contentListRepository.addShowToList(show.id, local)
                }
            }

        } catch (ignored: Exception){ Timber.e(ignored) }
    }
}
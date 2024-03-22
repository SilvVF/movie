package io.silv.movie.data.user

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
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
import io.silv.movie.data.movie.interactor.GetMovie
import io.silv.movie.data.movie.interactor.GetRemoteMovie
import io.silv.movie.data.movie.interactor.NetworkToLocalMovie
import io.silv.movie.data.movie.model.toDomain
import io.silv.movie.data.tv.interactor.GetRemoteTVShows
import io.silv.movie.data.tv.interactor.GetShow
import io.silv.movie.data.tv.interactor.NetworkToLocalTVShow
import io.silv.movie.data.tv.model.toDomain

/**
 * Toggles favorite for movies and shows from the list in supabase.
 * Fetches the movie or show if it does not exist already.
 * No deletion / removals of favorites happen only additions for current signed in user.
 */
class UserListUpdateWorker (
    private val contentListRepository: ContentListRepository,
    private val listRepository: ListRepository,
    private val auth: Auth,
    private val getShow: GetShow,
    private val getMovie: GetMovie,
    private val getRemoteTVShows: GetRemoteTVShows,
    private val getRemoteMovie: GetRemoteMovie,
    private val networkToLocalMovie: NetworkToLocalMovie,
    private val networkToLocalTVShow: NetworkToLocalTVShow,

    appContext: Context,
    private val params: WorkerParameters
): CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {

        val user = auth.currentUserOrNull()!!

        val network = listRepository.selectAllLists(user.id)!!

        for (list in network) {
            try {
                var local = contentListRepository.getListForSupabaseId(list.listId)

                if (local == null) {
                    val id = contentListRepository.createList(list.name, list.listId, list.userId, list.createdAt.toEpochMilliseconds())
                    local = contentListRepository.getList(id)!!
                }

                val items = listRepository.selectAllItemsForList(list.listId)!!

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
            } catch (ignored: Exception){}
        }
        return Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo()
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val id = "io.silv.FavoritesWorker"
        val title = "Refreshing favorites"
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
        const val TAG = "io.silv.UserListUpdateWorker"

        fun start(context: Context) {
            val workManager = WorkManager.getInstance(context)

            workManager.enqueueUniqueWork(
                TAG,
                ExistingWorkPolicy.REPLACE,
                workRequest()
            )
        }

        fun workRequest() = OneTimeWorkRequestBuilder<UserListUpdateWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag(TAG)
            .build()
    }
}
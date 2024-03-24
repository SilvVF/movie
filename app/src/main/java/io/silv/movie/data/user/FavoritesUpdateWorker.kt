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
import io.silv.movie.data.movie.interactor.UpdateMovie
import io.silv.movie.data.movie.model.toDomain
import io.silv.movie.data.movie.model.toMovieUpdate
import io.silv.movie.data.tv.interactor.GetRemoteTVShows
import io.silv.movie.data.tv.interactor.GetShow
import io.silv.movie.data.tv.interactor.NetworkToLocalTVShow
import io.silv.movie.data.tv.interactor.UpdateShow
import io.silv.movie.data.tv.model.toDomain
import io.silv.movie.data.tv.model.toShowUpdate
import io.silv.movie.presentation.library.screenmodels.FavoritesSortMode
import kotlinx.coroutines.flow.firstOrNull

/**
 * Toggles favorite for movies and shows from the list in supabase.
 * Fetches the movie or show if it does not exist already.
 * No deletion / removals of favorites happen only additions for current signed in user.
 */
class FavoritesUpdateWorker (
    private val contentListRepository: ContentListRepository,
    private val listRepository: ListRepository,
    private val auth: Auth,
    private val getShow: GetShow,
    private val getMovie: GetMovie,
    private val getRemoteTVShows: GetRemoteTVShows,
    private val getRemoteMovie: GetRemoteMovie,
    private val networkToLocalMovie: NetworkToLocalMovie,
    private val networkToLocalTVShow: NetworkToLocalTVShow,
    private val updateMovie: UpdateMovie,
    private val updateShow: UpdateShow,

    appContext: Context,
    private val params: WorkerParameters
): CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {

        val local = contentListRepository
            .observeFavorites("", FavoritesSortMode.RecentlyAdded)
            .firstOrNull()
            .orEmpty()

        val user = auth.currentUserOrNull()!!

        val network = listRepository.selectFavoritesList(user.id)!!

        val movieIds = network.mapNotNull { c -> c.movieId.takeIf { it != -1L } }.toSet()
        val showIds = network.mapNotNull { c ->  c.showId.takeIf { it != -1L } }.toSet()

        val needToAdd = local.filterNot {
            if (it.isMovie) it.contentId in movieIds else it.contentId in showIds
        }

        for (item in needToAdd) {
            if (item.isMovie) {
                runCatching {
                    val movie = getMovie.await(item.contentId)!!
                    listRepository.addMovieToFavoritesList(movie)
                }
            } else {
                runCatching {
                    val show = getShow.await(item.contentId)!!
                    listRepository.addShowToFavorites(show)
                }
            }
        }

        for (favorite in network) {
            try {
                if (favorite.movieId != -1L) {
                    var movie = getMovie.await(favorite.movieId)
                    if (movie == null) {
                        movie = networkToLocalMovie.await(
                            getRemoteMovie.awaitOne(favorite.movieId)!!.toDomain()
                        )
                    }

                    updateMovie.await(movie.copy(favorite = true).toMovieUpdate())
                } else if (favorite.showId != -1L) {
                    var show = getShow.await(favorite.showId)
                    if (show == null) {
                        show = networkToLocalTVShow.await(
                            getRemoteTVShows.awaitOne(favorite.showId)!!.toDomain()
                        )
                    }

                    updateShow.await(show.copy(favorite = true).toShowUpdate())
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
        const val TAG = "io.silv.FavoritesUpdateWorker"

        fun start(context: Context) {
            val workManager = WorkManager.getInstance(context)

            workManager.enqueueUniqueWork(
                TAG,
                ExistingWorkPolicy.REPLACE,
                workRequest()
            )
        }

        fun workRequest() = OneTimeWorkRequestBuilder<FavoritesUpdateWorker>()
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
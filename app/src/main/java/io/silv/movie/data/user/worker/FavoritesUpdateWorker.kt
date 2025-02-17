package io.silv.movie.data.user.worker

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
import io.silv.movie.data.content.lists.ContentListRepository
import io.silv.movie.data.content.movie.local.LocalContentDelegate
import io.silv.movie.data.content.movie.local.networkToLocalMovie
import io.silv.movie.data.content.movie.local.networkToLocalShow
import io.silv.movie.data.content.movie.model.toDomain
import io.silv.movie.data.content.movie.model.toMovieUpdate
import io.silv.movie.data.content.movie.model.toShowUpdate
import io.silv.movie.data.content.movie.network.NetworkContentDelegate
import io.silv.movie.data.user.repository.ListRepository
import io.silv.movie.presentation.screenmodel.FavoritesSortMode
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
    private val local: LocalContentDelegate,
    private val network: NetworkContentDelegate,
    appContext: Context,
    private val params: WorkerParameters
): CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {

        val localList = contentListRepository
            .observeFavorites("", FavoritesSortMode.RecentlyAdded)
            .firstOrNull()
            .orEmpty()

        val user = auth.currentUserOrNull()!!

        val networkList = listRepository.selectFavoritesList(user.id)!!

        val movieIds = networkList.mapNotNull { c -> c.movieId.takeIf { it != -1L } }.toSet()
        val showIds = networkList.mapNotNull { c ->  c.showId.takeIf { it != -1L } }.toSet()

        val needToAdd = localList.filterNot {
            if (it.isMovie) it.contentId in movieIds else it.contentId in showIds
        }

        for (item in needToAdd) {
            if (item.isMovie) {
                runCatching {
                    val movie = local.getMovieById(item.contentId)!!
                    listRepository.addMovieToFavoritesList(movie)
                }
            } else {
                runCatching {
                    val show = local.getShowById(item.contentId)!!
                    listRepository.addShowToFavorites(show)
                }
            }
        }

        for (favorite in networkList) {
            try {
                if (favorite.movieId != -1L) {
                    var movie = local.getMovieById(favorite.movieId)
                    if (movie == null) {
                        movie = local.networkToLocalMovie(
                            network.getMovie(favorite.movieId)!!.toDomain()
                        )
                    }
                    local.updateMovie(movie.copy(favorite = true).toMovieUpdate())
                } else if (favorite.showId != -1L) {
                    var show = local.getShowById(favorite.showId)
                    if (show == null) {
                        show = local.networkToLocalShow(
                            network.getShow(favorite.showId)!!.toDomain()
                        )
                    }
                    local.updateShow(show.copy(favorite = true).toShowUpdate())
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
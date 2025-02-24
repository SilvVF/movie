package io.silv.movie

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import cafe.adriel.voyager.navigator.Navigator
import io.silv.movie.presentation.ContentInteractor
import io.silv.movie.presentation.ContentInteractor.ContentEvent
import io.silv.movie.presentation.screen.ListViewScreen
import kotlinx.coroutines.flow.onEach


fun ContentInteractor.eventHandler(
    snackbarHostState: SnackbarHostState,
    context: Context,
    navigate: suspend (Navigator.() -> Unit) -> Unit
) = eventsAsFlow().onEach { event ->
    when (event) {
        is ContentEvent.AddToList -> {
            if (event.success) {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(
                        R.string.added_to_list,
                        event.item.title,
                        event.list.name
                    ),
                    actionLabel = context.getString(R.string.undo),
                    duration = SnackbarDuration.Short,
                )
                when (result) {
                    SnackbarResult.Dismissed -> Unit
                    SnackbarResult.ActionPerformed ->
                        removeFromList(event.list, event.item)
                }
            } else {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(
                        R.string.add_to_list_failed,
                        event.item.title,
                        event.list.name
                    ),
                    actionLabel = context.getString(R.string.retry),
                    duration = SnackbarDuration.Short,
                )
                when (result) {
                    SnackbarResult.Dismissed -> Unit
                    SnackbarResult.ActionPerformed -> addToList(event.list, event.item)
                }
            }
        }

        is ContentEvent.Favorite -> {
            if (event.success) {
                val result = snackbarHostState.showSnackbar(
                    message = if (event.item.favorite) {
                        context.getString(R.string.added_to_favorites, event.item.title)
                    } else context.getString(R.string.removed_from_favorites, event.item.title),
                    actionLabel = context.getString(R.string.undo),
                    duration = SnackbarDuration.Short,
                )
                when (result) {
                    SnackbarResult.Dismissed -> Unit
                    SnackbarResult.ActionPerformed -> toggleFavorite(event.item)
                }
            } else {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(
                        R.string.failed_to_change_favorite,
                        event.item.title
                    ),
                    actionLabel = context.getString(R.string.retry),
                    duration = SnackbarDuration.Short,
                )
                when (result) {
                    SnackbarResult.Dismissed -> Unit
                    SnackbarResult.ActionPerformed -> toggleFavorite(event.item)
                }
            }
        }

        is ContentEvent.RemoveFromList -> {
            if (event.success) {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(
                        R.string.removed_from_list,
                        event.item.title,
                        event.list.name
                    ),
                    actionLabel = context.getString(R.string.undo),
                    duration = SnackbarDuration.Short,
                )
                when (result) {
                    SnackbarResult.Dismissed -> Unit
                    SnackbarResult.ActionPerformed -> addToList(event.list, event.item)
                }
            } else {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(
                        R.string.remove_from_list_failed,
                        event.item.title,
                        event.list.name
                    ),
                    actionLabel = context.getString(R.string.retry),
                    duration = SnackbarDuration.Short,
                )
                when (result) {
                    SnackbarResult.Dismissed -> Unit
                    SnackbarResult.ActionPerformed -> removeFromList(event.list, event.item)
                }
            }
        }

        is ContentEvent.AddToAnotherList -> {
            if (event.success) {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(
                        R.string.added_to_list,
                        event.item.title,
                        event.list.name
                    ),
                    actionLabel = context.getString(R.string.take_me_there),
                    duration = SnackbarDuration.Short,
                )
                when (result) {
                    SnackbarResult.Dismissed -> Unit
                    SnackbarResult.ActionPerformed ->
                        navigate {
                            push(
                                ListViewScreen(
                                    event.list.id,
                                    event.list.supabaseId.orEmpty()
                                )
                            )
                        }
                }
            } else {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(
                        R.string.add_to_list_failed,
                        event.item.title,
                        event.list.name
                    ),
                    actionLabel = context.getString(R.string.retry),
                    duration = SnackbarDuration.Short,
                )
                when (result) {
                    SnackbarResult.Dismissed -> Unit
                    SnackbarResult.ActionPerformed -> addToList(event.list, event.item)
                }
            }
        }
    }
}
package io.silv.movie

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import cafe.adriel.voyager.navigator.Navigator
import io.silv.movie.presentation.ListInteractor
import io.silv.movie.presentation.ListInteractor.ListEvent
import io.silv.movie.presentation.screen.ListViewScreen
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.onEach


internal suspend fun ListInteractor.handleEvents(
    snackbarHostState: SnackbarHostState,
    context: Context,
    navigate: SendChannel<Navigator.() -> Unit>
) = eventsAsFlow().onEach { event ->
    when (event) {
        is ListEvent.Copied -> {
            if (event.success) {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(R.string.copied_list, event.list.name),
                    actionLabel = context.getString(R.string.take_me_there),
                    duration = SnackbarDuration.Short,
                )
                when (result) {
                    SnackbarResult.Dismissed -> Unit
                    SnackbarResult.ActionPerformed ->
                        navigate.send {
                            ListViewScreen(
                                event.list.id,
                                event.list.supabaseId.orEmpty()
                            )
                        }
                }
            } else {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(R.string.copied_list_failed, event.list.name),
                    actionLabel = context.getString(R.string.retry),
                    duration = SnackbarDuration.Short,
                )
                when (result) {
                    SnackbarResult.Dismissed -> Unit
                    SnackbarResult.ActionPerformed -> copyList(event.list)
                }
            }
        }

        is ListEvent.Delete -> {
            if (event.success) {
                snackbarHostState.showSnackbar(
                    message = context.getString(R.string.deleted_list, event.list.name),
                    duration = SnackbarDuration.Short
                )
            } else {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(R.string.delete_list_failed, event.list.name),
                    actionLabel = context.getString(R.string.retry),
                    duration = SnackbarDuration.Short,
                )
                when (result) {
                    SnackbarResult.Dismissed -> Unit
                    SnackbarResult.ActionPerformed -> deleteList(event.list)
                }
            }
        }

        is ListEvent.Subscribe -> {
            if (event.success) {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(R.string.subscribed_to_list, event.list.name),
                    duration = SnackbarDuration.Short,
                    actionLabel = context.getString(R.string.take_me_there),
                )
                when (result) {
                    SnackbarResult.Dismissed -> Unit
                    SnackbarResult.ActionPerformed ->
                        navigate.send {
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
                        R.string.subscribe_to_list_failed,
                        event.list.name
                    ),
                    actionLabel = context.getString(R.string.retry),
                    duration = SnackbarDuration.Short,
                )
                when (result) {
                    SnackbarResult.Dismissed -> Unit
                    SnackbarResult.ActionPerformed -> subscribeToList(event.list)
                }
            }
        }

        is ListEvent.VisibleChanged -> {
            if (event.success) {
                val result = snackbarHostState.showSnackbar(
                    message = if (event.list.public) {
                        context.getString(R.string.made_list_public)
                    } else {
                        context.getString(R.string.made_list_private)
                    },
                    actionLabel = context.getString(R.string.undo),
                    duration = SnackbarDuration.Short
                )
                when (result) {
                    SnackbarResult.Dismissed -> Unit
                    SnackbarResult.ActionPerformed ->
                        editList(event.list) { it.copy(public = !it.public) }
                }
            } else {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(
                        R.string.visiblity_change_failed,
                        event.list.name
                    ),
                    actionLabel = context.getString(R.string.retry),
                    duration = SnackbarDuration.Short,
                )
                when (result) {
                    SnackbarResult.Dismissed -> Unit
                    SnackbarResult.ActionPerformed ->
                        editList(event.list) { it.copy(public = !it.public) }
                }
            }
        }

        is ListEvent.Edited -> {
            if (event.success) {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(R.string.list_edited),
                    actionLabel = context.getString(R.string.undo),
                    duration = SnackbarDuration.Short
                )
                when (result) {
                    SnackbarResult.Dismissed -> Unit
                    SnackbarResult.ActionPerformed ->
                        editList(event.new) { event.original }
                }
            } else {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(R.string.edit_failed, event.original.name),
                    actionLabel = context.getString(R.string.retry),
                    duration = SnackbarDuration.Short,
                )
                when (result) {
                    SnackbarResult.Dismissed -> Unit
                    SnackbarResult.ActionPerformed ->
                        editList(event.original) { event.new }
                }
            }
        }

        is ListEvent.Unsubscribe -> {
            if (event.success) {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(
                        R.string.unsubsribed_from_list,
                        event.list.name
                    ),
                    duration = SnackbarDuration.Short,
                    actionLabel = context.getString(R.string.undo)
                )
                when (result) {
                    SnackbarResult.Dismissed -> Unit
                    SnackbarResult.ActionPerformed -> subscribeToList(event.list)
                }
            } else {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(
                        R.string.unsubsribed_from_list_failed,
                        event.list.name
                    ),
                    actionLabel = context.getString(R.string.retry),
                    duration = SnackbarDuration.Short,
                )
                when (result) {
                    SnackbarResult.Dismissed -> Unit
                    SnackbarResult.ActionPerformed -> unsubscribeFromList(event.list)
                }
            }
        }

        is ListEvent.Pinned -> {
            val result = snackbarHostState.showSnackbar(
                message = if (event.list.pinned) {
                    context.getString(R.string.pinned, event.list.name)
                } else {
                    context.getString(R.string.unpinned, event.list.name)
                },
                duration = SnackbarDuration.Short,
                actionLabel = context.getString(R.string.undo)
            )
            when (result) {
                SnackbarResult.Dismissed -> Unit
                SnackbarResult.ActionPerformed -> togglePinned(event.list)
            }
        }
    }
}
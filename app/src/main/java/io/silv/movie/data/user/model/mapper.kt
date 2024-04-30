package io.silv.movie.data.user.model

import io.silv.movie.data.user.model.list.ListItem
import io.silv.movie.data.user.model.list.ListWithItems
import io.silv.movie.data.user.model.subscription.SubscriptionWithItem
import kotlinx.datetime.Clock

fun List<SubscriptionWithItem>.toListWithItems(): List<ListWithItems> {
    return groupBy { it.listId }
        .map { (listId, items) ->
            val first = items.first()
            ListWithItems(
                listId = listId,
                userId = first.userId,
                description = first.description,
                public = first.public,
                name = first.name,
                createdAt = first.createdAt,
                updatedAt = first.updatedAt,
                subscribers = first.subscribers,
                items = items.mapNotNull {
                    if (it.movieId == null || it.showId == null) null
                    else ListItem(
                        listId,
                        it.userId,
                        it.movieId,
                        it.showId,
                        it.posterPath,
                        "",
                        null,
                        Clock.System.now()
                    )
                }
            )
        }
}
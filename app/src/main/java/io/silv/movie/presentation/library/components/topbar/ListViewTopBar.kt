package io.silv.movie.presentation.library.components.topbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import io.silv.core_ui.components.topbar.PosterLargeTopBar
import io.silv.core_ui.components.topbar.PosterTopBarState
import io.silv.core_ui.components.topbar.colors2
import io.silv.movie.R
import io.silv.movie.data.lists.ContentItem
import io.silv.movie.data.lists.ContentList
import io.silv.movie.data.prefrences.PosterDisplayMode
import io.silv.movie.data.user.User
import io.silv.movie.presentation.library.components.ContentListPosterItems
import io.silv.movie.presentation.library.screenmodels.ListSortMode
import io.silv.movie.presentation.profile.UserProfileImage
import kotlinx.collections.immutable.ImmutableList

@Composable
fun ListViewTopBar(
    state: PosterTopBarState,
    user: User?,
    isUserMe: Boolean,
    query: () -> String,
    changeQuery: (String) -> Unit,
    onSearch: (String) -> Unit,
    displayMode: () -> PosterDisplayMode,
    setDisplayMode: (PosterDisplayMode) -> Unit,
    onListOptionClicked: () -> Unit,
    contentListProvider: () -> ContentList,
    items: () -> ImmutableList<ContentItem>,
    sortModeProvider: () -> ListSortMode,
    changeSortMode: (ListSortMode) -> Unit,
    onPosterClick: () -> Unit,
    primary: Color,
    modifier: Modifier = Modifier,
) {
    val background = MaterialTheme.colorScheme.background
    val list = contentListProvider()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentSize()
            .drawWithCache {
                onDrawBehind {
                    drawRect(
                        Brush.verticalGradient(
                            colors = if (items().isEmpty()) {
                                listOf(primary, background)
                            } else {
                                listOf(primary, background)
                            },
                            endY = size.height * 0.8f
                        ),
                        alpha = if (state.isKeyboardOpen) 0f else 1f - state.progress
                    )
                }
            },
    ) {
        PosterLargeTopBar(
            state = state,
            title = {
                if (user != null) {
                    TitleWithProfilePicture(
                        user = user,
                        name = list.name,
                        description = list.description,
                    )
                } else {
                    Text(list.name)
                }
            },
            colors = TopAppBarDefaults.colors2(
                containerColor = Color.Transparent,
                scrolledContainerColor = primary.copy(alpha = 0.2f)
            ),
            smallTitle = {
                if (user != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserProfileImage(
                            user = user,
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .aspectRatio(1f),
                            contentDescription = null,
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            list.name,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                } else {
                    Text(list.name)
                }
            },
            navigationIcon = {
                PosterLargeTopBarDefaults.BackArrowIcon(
                    isKeyboardOpen = state.isKeyboardOpen
                )
            },
            actions = {
                PosterLargeTopBarDefaults.Actions(
                    displayMode = displayMode,
                    setDisplayMode = setDisplayMode,
                    onListOptionClicked = onListOptionClicked
                )
            },
            posterContent = {
                ContentListPosterItems(
                    list = list,
                    items = items(),
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .fillMaxHeight()
                        .clickable { onPosterClick() }
                )
            },
            pinnedContent = {
                AnimatedVisibility(visible = !state.isKeyboardOpen) {
                    ListFilterChips(
                        sortModeProvider = sortModeProvider,
                        changeSortMode = changeSortMode
                    )
                }
            }
        ) {
            PosterLargeTopBarDefaults.SearchInputField(
                query = query,
                onSearch = onSearch,
                changeQuery = changeQuery,
                placeholder = stringResource(id = R.string.search_placeholder, list.name)
            )
        }
    }
}

@Composable
fun TitleWithProfilePicture(
    user: User,
    name: String,
    description: String,
) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Bottom,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserProfileImage(
                user = user,
                error = painterResource(id = R.drawable.user_default_proflie_icon),
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .aspectRatio(1f),
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = user.username,
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.graphicsLayer {
                        alpha = 0.78f
                    }
                )
            }
        }
    }
}

@Composable
fun ListFilterChips(
    sortModeProvider: () -> ListSortMode,
    changeSortMode: (ListSortMode) -> Unit
) {
    val filters =
        remember {
            listOf(
                Triple(R.string.title, Icons.Filled.Title, ListSortMode.Title),
                Triple(R.string.recently_added, Icons.Filled.NewReleases, ListSortMode.RecentlyAdded),
                Triple(R.string.movies, Icons.Filled.Movie, ListSortMode.Movie),
                Triple(R.string.shows, Icons.Filled.Tv, ListSortMode.Show)
            )
        }

    val layoutDirection = LocalLayoutDirection.current

    val paddingHorizontal = with(LocalDensity.current) {
        TopAppBarDefaults.windowInsets.getLeft(this, layoutDirection).toDp() to
                TopAppBarDefaults.windowInsets.getRight(this, layoutDirection).toDp()
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = paddingHorizontal.first, end = paddingHorizontal.second),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val sortMode = sortModeProvider()
        filters.fastForEach { (tag, icon, type) ->
            item(key = tag) {
                ElevatedFilterChip(
                    modifier = Modifier.padding(4.dp),
                    selected = type == sortMode,
                    onClick = {
                        changeSortMode(type)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = icon.name,
                        )
                    },
                    label = {
                        Text(text = stringResource(tag))
                    },
                )
            }
        }
    }
}

